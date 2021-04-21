package iterator;

import bufmgr.PageNotReadException;
import global.*;
import heap.*;
import index.IndexException;
import tests.TableIndexDesc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static tests.Phase3Utils.getIndexesOnTable;

public class HashJoin extends Iterator {
    private AttrType outerTypes[], innerTypes[];
    private int nOuterRecs, nInnerRecs;
    private Iterator outer;
    private short innerStrSizes[];
    private short outerStrSizes[];
    private CondExpr OutputFilter[];
    private CondExpr RightFilter[];
    private int n_buf_pgs;        // # of buffer pages available.
    private boolean done;         // Is the join complete
    private Tuple outer_tuple, inner_tuple;
    private Tuple Jtuple;           // Joined tuple
    private FldSpec perm_mat[];
    private int nOutFlds;
    private Heapfile hf;
    private Scan inner;
    private String relation;
    private Scan joinScan;
    private ArrayList<Integer> oHashList, iHashList;
    private Iterator inlj;


    public HashJoin(AttrType in1[],
                    int len_in1,
                    short t1_str_sizes[],
                    AttrType in2[],
                    int len_in2,
                    short t2_str_sizes[],
                    int amt_of_mem,
                    Iterator am1,
                    String relationName,
                    CondExpr outFilter[],
                    CondExpr rightFilter[],
                    FldSpec proj_list[],
                    int n_out_flds
    ) throws IOException, NestedLoopException, HashJoinException {

        outerTypes = new AttrType[in1.length];
        innerTypes = new AttrType[in2.length];
        System.arraycopy(in1, 0, outerTypes, 0, in1.length);
        System.arraycopy(in2, 0, innerTypes, 0, in2.length);
        nOuterRecs = len_in1;
        nInnerRecs = len_in2;


        outer = am1;
        outerStrSizes = t1_str_sizes;
        innerStrSizes = t2_str_sizes;
        inner_tuple = new Tuple();
        Jtuple = new Tuple();
        OutputFilter = outFilter;
        RightFilter = rightFilter;

        n_buf_pgs = amt_of_mem;
        inner = null;
        done = false;

        AttrType[] Jtypes = new AttrType[n_out_flds];
        short[] t_size;

        perm_mat = proj_list;
        nOutFlds = n_out_flds;
        relation = relationName;
        joinScan = null;

        try {
            t_size = TupleUtils.setup_op_tuple(Jtuple, Jtypes,
                    in1, len_in1, in2, len_in2,
                    t1_str_sizes, t2_str_sizes,
                    proj_list, nOutFlds);
        } catch (TupleUtilsException e) {
            throw new HashJoinException(e, "TupleUtilsException is caught by NestedLoopsJoins.java");
        }

        try {
            hf = new Heapfile(relationName);
        } catch (Exception e) {
            throw new HashJoinException(e, "Create new heapfile failed.");
        }

        int joinFldInner = OutputFilter[0].operand2.symbol.offset;
        List<TableIndexDesc> indexList = getIndexesOnTable(relationName);
        boolean hashIndexExists = false;
        if (!indexList.isEmpty()) {
            for (TableIndexDesc desc : indexList) {
                if (desc.getAttributeIndex() == joinFldInner && desc.getType() == IndexType.Hash) {

                    hashIndexExists = true;
                }
            }
        }

        if (!hashIndexExists) {
            inlj = null;
            try {
                createInnerHashPartition();
                createOuterHashPartition();
                performHashJoin();
            } catch (Exception e) {
                throw new HashJoinException(e, "Perform hash join failed.");
            }
        } else {
            try {
                inlj = new IndexNestedLoopsJoins(in1, len_in1, t1_str_sizes, in2, len_in2, t2_str_sizes, amt_of_mem, am1, relationName,
                        outFilter, rightFilter, proj_list, n_out_flds);
            } catch (Exception e) {
                throw new HashJoinException(e, "Create IndexNestedLoopJoin iterator failed");
            }
        }
    }

    private void createInnerHashPartition() throws IOException, HFException, HFBufMgrException, HFDiskMgrException, HashJoinException {
        // Push all values in table into hash partitions after hashing
        Tuple data = null;
        RID rid = null;
        FileScan fscan = null;
        iHashList = new ArrayList<>();
        Heapfile bucketFile = null;

        FldSpec[] proj = new FldSpec[nInnerRecs];
        for (int i = 1; i <= nInnerRecs; i++) {
            proj[i - 1] = new FldSpec(new RelSpec(RelSpec.outer), i);
        }

        try {
            fscan = new FileScan(relation, innerTypes, innerStrSizes, (short) nInnerRecs, nInnerRecs, proj, null);
            data = fscan.get_next();
            data.setHdr((short) nInnerRecs, innerTypes, innerStrSizes);
            int joinFldInner = OutputFilter[0].operand2.symbol.offset;
            int bucket=-1;
            String bucket_name = "";

            while(data != null) {
                switch (innerTypes[joinFldInner - 1].attrType) {
                    case AttrType.attrInteger:
                        Integer iVal = data.getIntFld(joinFldInner);
                        bucket = get_hash(iVal);
                        break;
                    case AttrType.attrReal:
                        Float rVal = data.getFloFld(joinFldInner);
                        bucket = get_hash(rVal);
                        break;
                    case AttrType.attrString:
                        String sVal = data.getStrFld(joinFldInner);
                        bucket = get_hash(sVal);
                        break;
                    default:
                        break;
                }

                bucket_name = "inner_hash_bucket_"+bucket;
                if (!iHashList.contains(bucket)) {
                    iHashList.add(bucket);
                }

                bucketFile = new Heapfile(bucket_name);
                rid = bucketFile.insertRecord(data.getTupleByteArray());
                data = fscan.get_next();
            }
        } catch(Exception e) {
            throw new HashJoinException(e, "Create new heapfile failed.");
        }
    }

    private void createOuterHashPartition() throws IOException, HFException, HFBufMgrException, HFDiskMgrException, HashJoinException {
        Tuple data = null;
        RID rid = null;
        FileScan fscan = null;
        oHashList = new ArrayList<>();
        Heapfile bucketFile = null;

        FldSpec[] proj = new FldSpec[nOuterRecs];
        for (int i = 1; i <= nOuterRecs; i++) {
            proj[i - 1] = new FldSpec(new RelSpec(RelSpec.outer), i);
        }

        try {
            data = outer.get_next();
            data.setHdr((short) nOuterRecs, outerTypes, outerStrSizes);
            int joinFldOuter = OutputFilter[0].operand1.symbol.offset;
            int bucket=-1;
            String bucket_name = "";

            while(data != null) {
                switch (outerTypes[joinFldOuter - 1].attrType) {
                    case AttrType.attrInteger:
                        Integer iVal = data.getIntFld(joinFldOuter);
                        bucket = get_hash(iVal);
                        break;
                    case AttrType.attrReal:
                        Float rVal = data.getFloFld(joinFldOuter);
                        bucket = get_hash(rVal);
                        break;
                    case AttrType.attrString:
                        String sVal = data.getStrFld(joinFldOuter);
                        bucket = get_hash(sVal);
                        break;
                    default:
                        break;
                }

                bucket_name = "outer_hash_bucket_"+bucket;
                if (!oHashList.contains(bucket)) {
                    oHashList.add(bucket);
                }

                bucketFile = new Heapfile(bucket_name);
                rid = bucketFile.insertRecord(data.getTupleByteArray());

                data = outer.get_next();
            }
        } catch(Exception e) {
            throw new HashJoinException(e, "Create new heapfile failed.");
        }
    }

    private void performHashJoin() throws HashJoinException, HFDiskMgrException, InvalidSlotNumberException, InvalidTupleSizeException, HFBufMgrException, IOException {
        // Pick corresponding buckets
        Heapfile joinFile = null;
        Heapfile innerFile = null;
        Heapfile outerFile = null;
        FileScan outerScan = null;
        NestedLoopsJoins nlj = null;
        try {
            joinFile = new Heapfile("hashJoinFile.in");

            for (int hash : oHashList) {
                String innerFileName = "inner_hash_bucket_"+hash;
                innerFile = new Heapfile(innerFileName);
                String outerFileName = "outer_hash_bucket_"+hash;
                outerFile = new Heapfile(outerFileName);

                // Check if the buckets actually contain any tuples
                if(innerFile.getRecCnt() == 0 || outerFile.getRecCnt() == 0) {
                    continue;
                }

                FldSpec[] oProj = getProjection(nOuterRecs);

                outerScan = new FileScan(outerFileName, outerTypes, outerStrSizes, (short) nOuterRecs, nOuterRecs, oProj, null);

                // Perform NLJ
                nlj = new NestedLoopsJoins(outerTypes, nOuterRecs, outerStrSizes, innerTypes,
                        nInnerRecs, innerStrSizes, n_buf_pgs, outerScan,
                        innerFileName, OutputFilter, RightFilter, perm_mat,
                        nOutFlds);

                AttrType[] Jtypes = new AttrType[nOutFlds];
                Tuple t = new Tuple();
                TupleUtils.setup_op_tuple(t, Jtypes,
                        outerTypes, nOuterRecs, innerTypes, nInnerRecs,
                        outerStrSizes, innerStrSizes,
                        perm_mat, nOutFlds);

                Jtuple = nlj.get_next();
                while (Jtuple != null) {
                    t.tupleCopy(Jtuple);
                    joinFile.insertRecord(t.getTupleByteArray());
                    Jtuple = nlj.get_next();
                }

                nlj.close();
                outerScan.close();
            }
        } catch (Exception e) {
            throw new HashJoinException(e, "Create new heapfile failed.");
        }
    }

    public int get_hash(Object value) {
        return value.hashCode();
    }

    private FldSpec[] getProjection(int numFlds) {
        FldSpec[] proj = new FldSpec[numFlds];
        for (int j = 1; j <= numFlds; j++) {
            proj[j - 1] = new FldSpec(new RelSpec(RelSpec.outer), j);
        }
        return proj;
    }

    private void initJoinScan() {
        Heapfile joinFile = null;
        try {
            joinFile = new Heapfile("hashJoinFile.in");
            joinScan = new Scan(joinFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeBucketFiles() throws HashJoinException {
        Heapfile f = null;
        try {
            for (int hash : iHashList) {
                String fileName = "inner_hash_bucket_"+hash;
                f = new Heapfile(fileName);
                f.deleteFile();
            }

            for (int hash : oHashList) {
                String fileName = "outer_hash_bucket_"+hash;
                f = new Heapfile(fileName);
                f.deleteFile();
            }

            iHashList.clear();
            iHashList = null;

            f = new Heapfile("hashJoinFile.in");
            f.deleteFile();
        } catch (Exception e) {
            throw new HashJoinException(e, "Create new heapfile failed.");
        }
    }

    @Override
    public Tuple get_next() throws IOException, JoinsException, IndexException, InvalidTupleSizeException, InvalidTypeException, PageNotReadException, TupleUtilsException, PredEvalException, SortException, LowMemException, UnknowAttrType, UnknownKeyTypeException, Exception {
        if (inlj != null) {
            System.out.println("Used INLJ");
            return inlj.get_next();
        }

        if (done) {
            return null;
        }

        if (joinScan == null) {
            initJoinScan();
        }

        RID rid = new RID();
        Jtuple = joinScan.getNext(rid);
        if (Jtuple == null) {
            done = true;
            return null;
        }

        return Jtuple;
    }

    @Override
    public void close() throws IOException, JoinsException, SortException, IndexException {
        if (!closeFlag) {
            try {
                outer.close();
                closeBucketFiles();
            } catch (Exception e) {
                throw new JoinsException(e, "HashJoin.java: error in closing iterator.");
            }
            closeFlag = true;
        }
    }
}
