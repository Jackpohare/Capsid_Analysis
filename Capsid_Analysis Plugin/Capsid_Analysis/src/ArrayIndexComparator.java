import java.util.Comparator;

public class ArrayIndexComparator implements Comparator<Integer>
	{
	    private final double[] array;

	    public ArrayIndexComparator(double[] array)
	    {
	        this.array = array;
	    }

	    @Override
	    public int compare(Integer index1, Integer index2)
	    {
	         // Autounbox from Integer to int to use as array indexes
	        return (array[index1] == array[index2])?0
	        		:(array[index1] > array[index2])?1:-1;
	    }

	    public Integer[] createIndexArray()
	    {
	        Integer[] indexes = new Integer[array.length];
	        for (int i = 0; i < array.length; i++)
	        {
	            indexes[i] = i; // Autoboxing
	        }
	        return indexes;
	    }
	}

