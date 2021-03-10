package tests;

import btree.*;
import global.*;
import heap.Heapfile;
import heap.Scan;
import heap.Tuple;
import index.IndexScan;
import iterator.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class Player {
    public Float pid;
    public Float goals;
    public Float assists;

    public Player( Float _pid, Float _goals, Float _assists ) {
        pid = _pid;
        goals = _goals;
        assists = _assists;
    }
}

class BTreeSortedSkyDriver extends TestDriver implements GlobalConst {
    private int TRUE = 1;
    private int FALSE = 0;
    private boolean OK = true;
    private boolean FAIL = false;
    private static short REC_LEN1 = 32;
    private static short REC_LEN2 = 160;
    private List playersList;

    /**
     * BTreeSkyDriver Constructor, inherited from TestDriver
     */
    public BTreeSortedSkyDriver() {
        super("BTreeSortedSkyTest");
    }

    /**
     * calls the runTests function in TestDriver
     */
    public boolean runTests() {

        System.out.print("\n" + "Running " + testName() + " tests...." + "\n");

        try {
            SystemDefs sysdef = new SystemDefs(dbpath, NUMBUF + 20, NUMBUF, "Clock");
        } catch (Exception e) {
            Runtime.getRuntime().exit(1);
        }

        // Kill anything that might be hanging around
        String newdbpath;
        String newlogpath;
        String remove_logcmd;
        String remove_dbcmd;
        String remove_cmd = "/bin/rm -rf ";

        newdbpath = dbpath;
        newlogpath = logpath;

        remove_logcmd = remove_cmd + logpath;
        remove_dbcmd = remove_cmd + dbpath;

        // Commands here is very machine dependent.  We assume
        // user are on UNIX system here.  If we need to port this
        // program to other platform, the remove_cmd have to be
        // modified accordingly.
        try {
            Runtime.getRuntime().exec(remove_logcmd);
            Runtime.getRuntime().exec(remove_dbcmd);
        } catch (IOException e) {
            System.err.println("" + e);
        }

        remove_logcmd = remove_cmd + newlogpath;
        remove_dbcmd = remove_cmd + newdbpath;

        //This step seems redundant for me.  But it's in the original
        //C++ code.  So I am keeping it as of now, just in case
        //I missed something
        try {
            Runtime.getRuntime().exec(remove_logcmd);
            Runtime.getRuntime().exec(remove_dbcmd);
        } catch (IOException e) {
            System.err.println("" + e);
        }

        //Run the tests. Return type different from C++
        boolean _pass = runAllTests();

        //Clean up again
        try {
            Runtime.getRuntime().exec(remove_logcmd);
            Runtime.getRuntime().exec(remove_dbcmd);

        } catch (IOException e) {
            System.err.println("" + e);
        }

        System.out.print("\n" + "..." + testName() + " tests ");
        System.out.print(_pass == OK ? "completely successfully" : "failed");
        System.out.print(".\n\n");

        return _pass;
    }

    protected boolean runAllTests() {

        boolean _passAll = OK;

        //The following runs all the test functions

        //Running test1() to test6()
        if (!test1()) {
            _passAll = FAIL;
        }

        return _passAll;
    }

    public boolean test1() {
        System.out.println("------------------------ TEST 1 --------------------------");
        boolean status = OK;

// Add actual players list
        playersList = new ArrayList();
        int numPlayers = 12;

//        playersList.add( new Player( 101, 340, 190 ) );
//        playersList.add( new Player( 102, 460, 90 ) );
//        playersList.add( new Player( 103, 210, 240 ) );
//        playersList.add( new Player( 104, 200, 130 ) );
//        playersList.add( new Player( 105, 410, 70 ) );
//        playersList.add( new Player( 106, 320, 150 ) );
//        playersList.add( new Player( 107, 500, 50 ) );
//        playersList.add( new Player( 108, 120, 310 ) );
//        playersList.add( new Player( 109, 410, 70 ) );
//        playersList.add( new Player( 110, 20, 10 ) );
//        playersList.add( new Player( 111, 50, 20 ) );
//        playersList.add( new Player( 112, 110, 30 ) );


        playersList.add( new Player( 1.0f, 25.2f, 7.1f) );
        playersList.add( new Player( 17.3f, 70.0f, 1.1f) );
        playersList.add( new Player( 3.01f, 27.1f, 10.3f ) );
        playersList.add( new Player( 25.1f, 30.0f,  3.1f) );
        playersList.add( new Player( 2.5f, 35.5f, 2.0f ) );
        playersList.add( new Player( 35.1f, 40.1f, 3.6f ) );
        

        AttrType[] Ptypes = new AttrType[3];
        Ptypes[0] = new AttrType (AttrType.attrReal);
        Ptypes[1] = new AttrType (AttrType.attrReal);
        Ptypes[2] = new AttrType (AttrType.attrReal);

        Tuple t = new Tuple();
        try {
            t.setHdr((short) 3,Ptypes, null);
        }
        catch (Exception e) {
            System.err.println("*** error in Tuple.setHdr() ***");
            status = FAIL;
            e.printStackTrace();
        }

        int size = t.size();

        RID rid;
        Heapfile f = null;
        try {
            f = new Heapfile("playersnew.in");
        }
        catch (Exception e) {
            System.err.println("*** error in Heapfile constructor ***");
            status = FAIL;
            e.printStackTrace();
        }

        t = new Tuple(size);
        try {
            t.setHdr((short) 3, Ptypes, null);
        }
        catch (Exception e) {
            System.err.println("*** error in Tuple.setHdr() ***");
            status = FAIL;
            e.printStackTrace();
        }

        for (int i=0; i<playersList.size(); i++) {
            try {
                t.setFloFld(1, ((Player)playersList.get(i)).pid);
                t.setFloFld(2, ((Player)playersList.get(i)).goals);
                t.setFloFld(3, ((Player)playersList.get(i)).assists);
            }
            catch (Exception e) {
                System.err.println("*** Heapfile error in Tuple.setStrFld() ***");
                status = FAIL;
                e.printStackTrace();
            }

            try {
                rid = f.insertRecord(t.returnTupleByteArray());
            }
            catch (Exception e) {
                System.err.println("*** error in Heapfile.insertRecord() ***");
                status = FAIL;
                e.printStackTrace();
            }


        }
        if (status != OK) {
            //bail out
            System.err.println ("*** Error creating relation for sailors");
            Runtime.getRuntime().exit(1);
        }

        FldSpec[] Pprojection = new FldSpec[3];
        Pprojection[0] = new FldSpec(new RelSpec(RelSpec.outer), 1);
        Pprojection[1] = new FldSpec(new RelSpec(RelSpec.outer), 2);
        Pprojection[2] = new FldSpec(new RelSpec(RelSpec.outer), 3);

        // Scan the players table
        FileScan am = null;
        try {
            am  = new FileScan("players.in", Ptypes, null,
                    (short)3, (short)3,
                    Pprojection, null);
        }
        catch (Exception e) {
            status = FAIL;
            System.err.println (""+e);
        }
        if (status != OK) {
            //bail out
            System.err.println ("*** Error setting up scan for players");
            Runtime.getRuntime().exit(1);
        }

        // create an scan on the heapfile
        Scan scan = null;

        try {
            scan = new Scan(f);
        } catch (Exception e) {
            status = FAIL;
            e.printStackTrace();
            Runtime.getRuntime().exit(1);
        }

        int[] pref_list = new int[2];
        pref_list[0] = 2;
        pref_list[1] = 3;

        // create the index file
        BTreeFile btf1 = null;
        try {
            btf1 = new BTreeFile("BTreeIndexNew", AttrType.attrReal, REC_LEN1, 1/*delete*/);
        } catch (Exception e) {
            status = FAIL;
            e.printStackTrace();
            Runtime.getRuntime().exit(1);
        }

        System.out.println("BTreeIndex created successfully.\n");

        rid = new RID();
        Float key = null;
      

        Tuple temp = null;

        try {
            temp = scan.getNext(rid);
        } catch (Exception e) {
            status = FAIL;
            e.printStackTrace();
        }
        Float flo = (Float)0.1f;
        while (temp != null) {
            t.tupleCopy(temp);

            try {
                key = t.getFloFld( pref_list[0]) + t.getFloFld( pref_list[1]) ;
           
            } catch (Exception e) {
                status = FAIL;
                e.printStackTrace();
            }

            try {
                btf1.insert(new FloatKey(key), rid);
            } catch (Exception e) {
                status = FAIL;
                e.printStackTrace();
            }

            try {
                temp = scan.getNext(rid);
            } catch (Exception e) {
                status = FAIL;
                e.printStackTrace();
            }
        }
        try {
            BT.printAllLeafPages(btf1.getHeaderPage());
        }catch (Exception e){
            e.printStackTrace();
        }

        IndexFile[] indexFiles = new IndexFile[1];
        indexFiles[0] = btf1;
        



        
        //Get skyline elements
        BTreeSortedSky bTreeSortedSky = null;
        try {
            bTreeSortedSky = new BTreeSortedSky(Ptypes, 3, null, 1000, am, "playersnew.in", pref_list, null, indexFiles,  100);
        } catch (Exception e) {
            System.err.println ("*** Error preparing for btree sorted sky");
            System.err.println (""+e);
            e.printStackTrace();
            Runtime.getRuntime().exit(1);
        }

        t = new Tuple();
        int count = 0;
        try {
            System.out.println("setsky: calling");
            t = bTreeSortedSky.get_next();
        }
        catch (Exception e) {
            status = FAIL;
            e.printStackTrace();
        }

        System.out.println("-----------SkylineSS for given dataset--------------");
        while( t != null ) {
            System.out.println("setsky: PRINT");
            try {
                System.out.println(t.getFloFld(1) + " -- " + t.getFloFld(2) + " " + t.getFloFld(3));
                t = bTreeSortedSky.get_next();
            } catch (Exception e) {
                status = FAIL;
                e.printStackTrace();
            }
        }
        if(t==null) {
            System.out.println("setsky: all null");
        }
        System.out.println("-----------End Skyline--------------");

        //clean up
        try {
            bTreeSortedSky.close();
        }
        catch (Exception e) {
            status = FAIL;
            e.printStackTrace();
        }

        
        System.out.println("------------------- TEST 1 completed ---------------------\n");
        return status;
    }

    /**
     * overrides the testName function in TestDriver
     *
     * @return the name of the test
     */
    protected String testName() {
        return "BTreeSortedSky";
    }
}

public class BTreeSortedSkyTest {
    public static void main(String[] args) {

        BTreeSortedSkyDriver nlsDriver = new BTreeSortedSkyDriver();
        boolean dbstatus;

        dbstatus = nlsDriver.runTests();

        if (!dbstatus) {
            System.out.println("Error encountered during Dominates tests:\n");
            Runtime.getRuntime().exit(1);
        }

        Runtime.getRuntime().exit(0);
    }
}