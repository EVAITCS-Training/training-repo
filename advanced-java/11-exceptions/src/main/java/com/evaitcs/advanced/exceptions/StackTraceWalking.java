package com.evaitcs.advanced.exceptions;

public class StackTraceWalking {

    public static void main(String[] args) {
        try {
            // Step 1: Call a method chain so we build a visible call stack.
            controllerLayer();
        } catch (IllegalStateException ex) {
            // Step 4: Catch the exception at a higher level and inspect it.
            System.out.println("Caught: " + ex);
            System.out.println();

            // Step 5: Pull the snapshot of stack frames stored on the exception.
            StackTraceElement[] frames = ex.getStackTrace();

            System.out.println("Walking stack trace from top frame to bottom:");
            for (int i = 0; i < frames.length; i++) {
                StackTraceElement frame = frames[i];

                // Step 6: Read useful metadata from each frame.
                System.out.printf(
                        "#%d class=%s method=%s file=%s line=%d%n",
                        i,
                        frame.getClassName(),
                        frame.getMethodName(),
                        frame.getFileName(),
                        frame.getLineNumber()
                );
            }

            // Optional teaching point:
            // The first frame is where the exception was created (closest to the bug),
            // and later frames show who called whom until main().
        }
    }

    private static void controllerLayer() {
        // Step 2: Simulate layers in an application (controller -> service -> repository).
        serviceLayer();
    }

    private static void serviceLayer() {
        repositoryLayer();
    }

    private static void repositoryLayer() {
        // Step 3: Throw an exception at the deepest layer.
        throw new IllegalStateException("Simulated data access failure");
    }
}
