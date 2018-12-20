import java.util.Arrays;
import java.util.Comparator;

import ij.IJ;
import ij.measure.ResultsTable;
import ij.text.TextPanel;


public class ResultsTable2 extends ResultsTable {
	
	class tableComparator implements Comparator<String[]> {
		private int columnToSortOn;
		private boolean ascending;
		boolean bIsStringCol;

		// contructor to set the column to sort on.
		tableComparator(int columnToSortOn, boolean ascending, boolean bIsStringCol) {
			this.columnToSortOn = columnToSortOn;
			this.ascending = ascending;
			this.bIsStringCol = bIsStringCol;
		}

		// Implement the abstract method which tells
		// how to order the two elements in the array.
		public int compare(String[] o1, String[] o2) {
			String[] row1 = (String[]) o1;
			String[] row2 = (String[]) o2;
			int res;
			if (!bIsStringCol) {
			if (Double.parseDouble(row1[columnToSortOn]) == Double.parseDouble(row2[columnToSortOn]))
				return 0;
			if (Double.parseDouble(row1[columnToSortOn]) > Double.parseDouble(row2[columnToSortOn]))
				res = 1;
			else
				res = -1;
			} else {
				res = row1[columnToSortOn].compareTo(row2[columnToSortOn]);
			}
			if (ascending)
				return res;
			else
				return (-1) * res;

		}
	}

    /** Sorts this table on the specified column. TO DO: add string support.*/
    public void sort(String column) {
        int col = getColumnIndex(column);
        if (col==COLUMN_NOT_FOUND)
            throw new IllegalArgumentException("Column not found");
        
        // Check if sorting on a string
        String s = getStringValue(col, 0);
  
        sortOn(col);
        
    }
    
    protected void sortOn(int col) {

 		int colNumber;
		String cellValue;
		int len;
		int i, j;
		boolean bSortColumnIsString = false;

		colNumber = getLastColumn() + 1;
		// IJ.log("colNumber=" + colNumber);
		float[] s = getColumn(0);
		len = s.length;
		// IJ.log("length=" + len);
		String[][] data = new String[len][colNumber];
		for (i = 0; i < len; i++) {
			for (j = 0; j < colNumber; j++) {
				data[i][j] = "";
			}
		}

		for (i = 0; i < colNumber; i++) {
			boolean bIsString = false;
			// Test for string column
			cellValue = getStringValue(i, 0);
			 if (  !utils.isDouble(cellValue)) {
				 bIsString = true;
			 }
			 if (i == col) {
				 bSortColumnIsString = bIsString;
			 }
			for (j = 0; j < len; j++) {
				cellValue = (bIsString) ? getStringValue(i, j) : String.format("%.4f", getValueAsDouble(i, j));
				data[j][i] = cellValue;
			}
		}


		Arrays.sort(data, new tableComparator(col, false, bSortColumnIsString));

		for (i = 0; i < colNumber; i++) {
			boolean bIsString = false;
			// Test for string column
			cellValue = data[0][i];
			 if (  !utils.isDouble(cellValue)) {
				 bIsString = true;
			 }
			// IJ.log("Updating column " + i);
			for (j = 0; j < len; j++) {
				if (bIsString )
					setValue(i, j, data[j][i]);
				else
					setValue(i, j, Double.parseDouble(data[j][i]));
			}
		}

		show("Results");
		IJ.showStatus("Results sort complete");
		// rt.getResultsWindow().getTextPanel().scrollToTop();
		TextPanel rtp = IJ.getTextPanel();
		if (rtp != null) {
			rtp.scrollToTop();
		} else {
			IJ.log("Could not get tesults text panel");
		}

    }
}
