package tests;

import bufmgr.PageNotReadException;
import catalog.AttrDesc;
import catalog.IndexDesc;
import catalog.RelDesc;
import global.AttrType;
import global.ExtendedSystemDefs;
import global.IndexType;
import global.SystemDefs;
import heap.*;
import iterator.*;

import java.io.IOException;
import java.util.List;

public class Phase3Utils {
    public static void writeToDisk() {
        try {
            SystemDefs.JavabaseBM.flushAllPages();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void insertIntoTable(List<Tuple> skylineElementsOfEachGroup, Heapfile materHeapfile) throws HFDiskMgrException, InvalidTupleSizeException, HFException, IOException, InvalidSlotNumberException, SpaceNotAvailableException, HFBufMgrException {
        for (Tuple tuple : skylineElementsOfEachGroup) {
            materHeapfile.insertRecord(tuple.returnTupleByteArray());
        }
    }

    public static IteratorDesc getTableItr(String tableName) throws IOException, HFException, HFBufMgrException, HFDiskMgrException, InvalidTupleSizeException, FileScanException, TupleUtilsException, InvalidRelation, PredEvalException, JoinsException, FieldNumberOutOfBoundException, PageNotReadException, InvalidTypeException, WrongPermat, UnknowAttrType {
        int numAttr = 0;

        if (!Phase3Driver.isTableInDB(tableName)) {
            System.err.println("*** error: relation " + tableName + " not found in DB");
            return null;
        }
        RelDesc rec = new RelDesc();
        try {
            ExtendedSystemDefs.MINIBASE_RELCAT.getInfo(tableName, rec);
            numAttr = rec.getAttrCnt();
            if (numAttr == 0) {
                System.err.println("*** error: catalog attribute count is 0 ");
                return null;
            }
        } catch (Exception e) {
            System.err.println("*** error: " + e);
            return null;
        }
        AttrType[] attrTypes = new AttrType[numAttr];
        for (int i = 0; i < attrTypes.length; ++i) {
            attrTypes[i] = new AttrType(AttrType.attrNull);
        }
        short[] strSizes = new short[numAttr];

        try {
            ExtendedSystemDefs.MINIBASE_ATTRCAT.getTupleStructure(tableName, numAttr, attrTypes, strSizes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return new IteratorDesc(tableName, (short) numAttr, attrTypes, strSizes);
    }

    public static void checkIndexesOnTable(String relName, int nFlds, int attr, int indexCnt, IndexDesc[] indexDescList) {
        AttrDesc[] attrDescs = new AttrDesc[nFlds];
        try {
            ExtendedSystemDefs.MINIBASE_ATTRCAT.getRelInfo(relName, nFlds, attrDescs);
            String attrName = attrDescs[attr-1].attrName;
            ExtendedSystemDefs.MINIBASE_INDCAT.getAttrIndexes(relName, attrName, indexCnt, indexDescList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean aggListContainsStringAttr(FldSpec[] agg_list, AttrType[] type) {
        for (FldSpec attr : agg_list) {
            if (!(type[attr.offset - 1].attrType == AttrType.attrInteger || type[attr.offset - 1].attrType == AttrType.attrReal)) {
                return true;
            }
        }
        return false;
    }
}