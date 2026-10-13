package com.evaitcs.advanced.exceptions;

public class FinallyBlockExample {

    public static void main(String[] args) {
        finallyShutdownFail();
    }

    static void finallyRun() {
        try {
            System.out.println("Try Block");
            return;
        } catch (Exception e) {
            System.out.println("Catch Block");
        } finally {
            System.out.println("finally Block");
        }
    }

    static void finallyExceptionRun() {
        try {
            throw new Exception("Exception Thrown");
        } catch (Exception e) {
            System.out.println("Catch Block");
        } finally {
            System.out.println("finally Block");
        }
    }

    static void finallyShutdownFail() {
        try {
            System.out.println("Try Block");
            System.exit(0);
        } catch (Exception e) {
            System.out.println("Catch Block");
        } finally {
            System.out.println("finally Block");
        }
    }
}
