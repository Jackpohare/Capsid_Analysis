import ij.IJ;
import ij.measure.*;


public class ResultsTable2 extends ResultsTable {
    /** Sorts this table on the specified column. TO DO: add string support.*/
    public void sort(String column) {
    	IJ.log("My sort");
        int col = getColumnIndex(column);
        if (col==COLUMN_NOT_FOUND)
            throw new IllegalArgumentException("Column not found");
        double[] values = new double[size()];
        
        // Check if sorting on a string
        String s = getStringValue(col, 0);
        if ( !utils.isDouble(s)) {
        	return;
        }
        super.sort(column);
    }
}
