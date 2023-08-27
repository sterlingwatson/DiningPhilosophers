import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.io.FileWriter;
import java.io.IOException;

class Philosopher extends Thread {
    private int id;
    private Lock leftFork; 
    private Lock rightFork;
    private boolean timesUp = false;
    private Random random;
    private FileWriter writer;
    private long startTime;
    private long hungryDuration;
    private long thinkingDuration;
    private long eatingDuration;

    public Philosopher(int id, Lock leftFork, Lock rightFork, FileWriter writer, long startTime) {
        this.id = id;
        this.leftFork = leftFork;
        this.rightFork = rightFork;
        this.random = new Random();
        this.writer = writer;
        this.startTime = startTime;
        this.hungryDuration = (long) 0.00;
        this.thinkingDuration = (long) 0.00;
        this.eatingDuration = (long) 0.00;
    }

    public long gethungryDuration() {
        return hungryDuration;
    }

    public long getthinkingDuration() {
        return thinkingDuration;
    }

    public long geteatingDuration() {
        return eatingDuration;
    }

    public long getsleepDuration() {
        return hungryDuration - eatingDuration;
    }

    private synchronized void log(String message) {
        long elapsedTime = System.currentTimeMillis() - startTime;
        String log = String.format("Philosopher: %d, Time: %d ms, %s\n", id, elapsedTime, message);
        try {
            writer.write(log);
            writer.flush();
        } catch (IOException e) {
            System.out.println("Error closing file in log()");
            e.printStackTrace();
        }
    }

    private void eat() throws InterruptedException {
        log("entering hungry state");
        long hungryStart = System.currentTimeMillis();
        while (!timesUp) {
            if (leftFork.tryLock()){
                try{
                    log("picked up fork " + id);
                    if (rightFork.tryLock()) {
                        try{
                            log("picked up fork " + ((id + 1) % 4));
                            hungryDuration += System.currentTimeMillis() - hungryStart;
                            int hungryRandom = (random.nextInt(31) + 10);
                            log(String.format("entering eating state. Will eat for %d ms.", hungryRandom));
                            Thread.sleep(hungryRandom);
                            eatingDuration += System.currentTimeMillis() - (hungryStart + hungryRandom);
                            break;
                        } finally {
                            rightFork.unlock();
                        }
                    }
                } finally {
                    leftFork.unlock();
                }
            }
            Thread.sleep(random.nextInt(51) + 50);
        }
    }

    private void think() throws InterruptedException {
        log("entering thinking state");
        Thread.sleep(10);
        thinkingDuration += 10;
    }

    public void run() {
        try {
            long endTime = System.currentTimeMillis() + 60000;
            while (System.currentTimeMillis() < (endTime)) {
                think();
                eat();
            }
            timesUp = true;
            hungryDuration = 60000 - (thinkingDuration + eatingDuration);
            System.out.printf("Philosopher %d: hungry: %dms, eatingTime: %dms, thinking: %dms\n", id, hungryDuration, eatingDuration, thinkingDuration);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } 
    }
}

public class DiningPhilosophersQuestion1 {
    public static void main(String[] args) {
        Lock[] forks = new Lock[4];
        for (int i = 0; i < forks.length; i++) {
            forks[i] = new ReentrantLock();
        }

        try {
            FileWriter writer = new FileWriter("philosophers.log");
            long startTime = System.currentTimeMillis();
            Philosopher[] philosophers = new Philosopher[4];
            for (int i = 0; i < philosophers.length; i++) {
                philosophers[i] = new Philosopher(i, forks[i], forks[(i + 1) % 4], writer, startTime);
                philosophers[i].start();
                
            }
            for (Philosopher philosopher : philosophers) {
                philosopher.join();
            }
            long[] stateDuration = new long[3];
            long totalTime = System.currentTimeMillis() - startTime;
            for (Philosopher philosopher : philosophers) {
                stateDuration[0] += philosopher.gethungryDuration();
                stateDuration[1] += philosopher.geteatingDuration();
                stateDuration[2] += philosopher.getthinkingDuration();
                totalTime -= philosopher.getthinkingDuration();
                totalTime -= philosopher.getsleepDuration();
            }
            int totalPhilosophers = philosophers.length;
            writer.write(String.format("Average time spent hungry: %dms\n", stateDuration[0] / totalPhilosophers));
            writer.write(String.format("Average time spent eating: %dms\n", stateDuration[1] / totalPhilosophers));
            writer.write(String.format("Average time spent thinking: %dms\n", stateDuration[2] / totalPhilosophers));
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
