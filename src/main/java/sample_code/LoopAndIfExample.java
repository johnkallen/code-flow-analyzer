package sample_code;

public class LoopAndIfExample {

    /**
     * Iterates through numbers from 1 to a given limit and checks if each is even or odd.
     *
     * @param limit The upper bound for the loop (inclusive).
     */
    public static void checkEvenOdd(int limit) {
        System.out.println("--- Checking numbers up to " + limit + " ---");

        // The for loop iterates from i = 1 up to the specified limit
        for (int i = 1; i <= limit; i++) {
            // The if statement checks a condition for each number 'i'
            if (i % 2 == 0) {
                // This block executes if 'i' is an even number
                System.out.println(i + " is an even number.");
            } else {
                // This block executes if the condition is false (i is odd)
                System.out.println(i + " is an odd number.");
            }
        }
    }

    public static void main(String[] args) {
        // Call the method to demonstrate the functionality
        checkEvenOdd(5);
    }
}