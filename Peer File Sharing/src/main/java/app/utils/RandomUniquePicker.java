package app.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * The RandomUniquePicker class contains a method to randomly pick a specified number of unique items from an array.
 */
public class RandomUniquePicker {

    /**
     * Randomly picks 'n' unique items from the provided array of items.
     * If 'n' is greater than the length of the array, all items are returned in a Set.
     *
     * @param items The array of items to pick from.
     * @param n The number of unique items to pick.
     * @return A Set containing 'n' randomly picked unique items.
     */
    public static Set<String> pick(String[] items, int n) {
        // If the number of items to pick is greater than the array length, return all items as a Set
        if (n > items.length) {
            return new HashSet<>(Arrays.asList(items));
        }

        Random random = new Random();

        // Shuffle the array using the Fisher-Yates shuffle algorithm
        for (int i = items.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            String temp = items[i];
            items[i] = items[j];
            items[j] = temp;
        }

        // Select the first 'n' items of the shuffled array
        String[] uniqueItems = Arrays.copyOfRange(items, 0, n);
        Set<String> set = new HashSet<>(Arrays.asList(uniqueItems));

        return set;
    }
}
