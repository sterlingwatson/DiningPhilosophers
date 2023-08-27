import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.Semaphore;
import java.io.FileWriter;
import java.io.IOException;

class Philosopher2 extends Thread {
    private int id;
    final int NUM_PHILOSOPHERS = 4;
    int LEFT;// = (id+NUM_PHILOSOPHERS-1)%NUM_PHILOSOPHERS;   // number of i's left neighbor
    int RIGHT;// = (id+1)%NUM_PHILOSOPHERS;                   // number of i's right neighbor
    final int THINKING = 0;
    final int HUNGRY = 1;
    final int EATING = 2;
    private FileWriter writer;
    private Random random;
    private long startTime;
    private long hungryStart;
    private long hungryDuration;
    private long thinkingDuration;
    private long eatingDuration;
    
    int state[] = new int[NUM_PHILOSOPHERS];                    // track state of each philosopher
    private Lock mutex = new ReentrantLock();                         // mutual exclusion for critical regions
    Semaphore s[] = new Semaphore[NUM_PHILOSOPHERS];            // one semaphore per philosopher

    public Philosopher2(int id, FileWriter writer, long startTime) {
        this.id = id;
        this.random = new Random();
        this.writer = writer;
        this.startTime = startTime;
        this.hungryDuration = 0L;
        this.thinkingDuration = 0L;
        this.eatingDuration = 0L;
        init();
    }

    private void log(String message) {
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

    void init() {
        LEFT = (id + NUM_PHILOSOPHERS - 1) % NUM_PHILOSOPHERS;
        RIGHT = (id + 1) % NUM_PHILOSOPHERS;
        for( int i = 0; i < NUM_PHILOSOPHERS; i++ ) {
            s[i] = new Semaphore(0);
        }
    }

    void think() throws InterruptedException {
        state[id] = THINKING;
        log("entering thinking state");
        Thread.sleep(10);
        thinkingDuration += 10;
    }

    void take_forks(int id) {
        mutex.lock();
        try {
            state[id] = HUNGRY;
            test(id);
            s[id].acquireUninterruptibly();
        } finally {
            mutex.unlock();
        }
        put_forks(id);
    }

    void put_forks(int id) {
        mutex.lock();
        try {
            state[id] = THINKING;
            test(LEFT);
            test(RIGHT);
        }finally{
            mutex.unlock();
        }
    }

    void eat(long hungryStart) {
        int hungryRandom = (random.nextInt(31) + 10);
        log(String.format("entering eating state. Will eat for %d ms.", hungryRandom));
        synchronized(this) {
            try{
                Thread.sleep(hungryRandom);
            } catch (InterruptedException e) {
                System.out.println("Error in eat");
                e.printStackTrace();
            }
            eatingDuration += hungryRandom;
        }
    }

    void test(int id) {
        if (state[id] == HUNGRY && state[LEFT] != EATING && state[RIGHT] != EATING) {
            state[id] = EATING;
            s[id].release();
            log("acquired forks " + LEFT + " and " + RIGHT);
        }
    }

    public void run() {            
        long endTime = System.currentTimeMillis() + 60000;

        while (System.currentTimeMillis() < endTime) {
            
            try {
                think();
            } catch (InterruptedException e) {
                System.out.println("Error in run");
                e.printStackTrace();
            }
            log("finished thinking, getting hungry");
            state[id] = HUNGRY;
            take_forks(id);
            eat(hungryStart);
            put_forks(id);
        }
        hungryDuration = 60000 - (eatingDuration + thinkingDuration); 
        System.out.printf("Philosopher %d: hungry: %dms, eatingTime: %dms, thinking: %dms\n", id, hungryDuration, eatingDuration, thinkingDuration);
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
}

public class DiningPhilosophersQuestion2 {
    public static void main(String[] args) {
        Lock[] forks = new Lock[4];
        for (int i = 0; i < forks.length; i++) {
            forks[i] = new ReentrantLock();
        }

        try {
            FileWriter writer = new FileWriter("philosophers.log");
            long startTime = System.currentTimeMillis();
            Philosopher2[] philosophers = new Philosopher2[4];
            for (int i = 0; i < philosophers.length; i++) {
                philosophers[i] = new Philosopher2(i, writer, startTime);
                philosophers[i].start();
                
            }
            for (Philosopher2 philosopher : philosophers) {
                philosopher.join();
            }
            long[] stateDuration = new long[3];
            long totalTime = System.currentTimeMillis() - startTime;
            for (Philosopher2 philosopher : philosophers) {
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