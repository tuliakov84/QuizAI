package org.example;

import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

public class Main {

    public static void main(String[] args) throws Exception {

        String jsonString = "[{\"topic\":\"Ocean\",\"n\":10,\"difficult\":2}]";

        CompletableFuture<String> futureJson =
                QuestionGenerator.generate(jsonString);

        calculator();

        String resultJson = futureJson.join();

        System.out.println(resultJson);
    }

    public static void  calculator() {

        Scanner scanner = new Scanner(System.in);

        for (int i = 0; i < 5; i++) {
            int a = scanner.nextInt();
            int b = scanner.nextInt();

            System.out.println(a + b);
        }
    }
}