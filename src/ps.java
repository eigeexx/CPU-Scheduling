import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Math.min;

/** schedule processes **/
class prioritySchedule extends TimerTask {
    private final process process[]; static int isNotP;
    int time = 0; int currentBT, currentID, currentPT, prevID = Integer.MAX_VALUE;

    prioritySchedule (process process[], boolean isNotPreemptive) {
        this.process = process;
        if (isNotPreemptive == false) { isNotP = 0; } else { isNotP = 1; }
        currentBT = Integer.MAX_VALUE; currentPT = Integer.MAX_VALUE;
    }

    public void run() {
        //System.out.printf(java.time.LocalTime.now()+"\t");
        //start scheduling at first arrivalTime
        if (time >= process[0].aT) {
            completeTask(time);
        } else {
            try {
                //System.out.printf("No Process Yet\n");
                System.out.printf("|\t");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        time++;
    }

    private void completeTask(int time) {
        try {
            //only traverse through processes with burstTime > 0
            for (int i=0; i<process.length; i++) {
                if(prioritySchedule.isNotP == 0) { //preemptive
                    if (process[i].aT <= time && process[i].pT < currentPT && process[i].bT > 0) { //compare base on arrival time & priority
                        currentBT = process[i].bT; currentID = i; currentPT = process[i].pT;
                    }
                } else { //non-preemptive
                    if (process[i].bT > 0) {
                        currentBT = process[i].bT; currentID = i;
                        break;
                    }
                }
            }

            process[currentID].reduceBurstTime(); process[currentID].addExecutionTime(); //reduce burst time and add execution time
            currentBT = process[currentID].bT; //check current burst time
            if (currentBT == 0) { //if 0, reset its value for next process
                process[currentID].setCompletionTime(time+1);
                currentBT = Integer.MAX_VALUE; currentPT = Integer.MAX_VALUE;
            }

            //print gantt chart
            //isntead of: System.out.printf("Running Process P"+process[currentID].p+"...\n");
            if (currentID == prevID) { //if same process
                System.out.printf("\t");
            } else { //if process was changed
                System.out.printf("|   P"+process[currentID].p+"\t");
                prevID = currentID;
            }

            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

public class ps {
    //startSchedule: start the timer, start scheduling processes
    public void startSchedule(process process[], int completionTime, boolean isNotPreemptive) {
        //Reference: https://www.journaldev.com/1050/java-timer-timertask-example
        Timer timer = new Timer(true);
        timer.schedule(new prioritySchedule(process, isNotPreemptive), 0, 1000); //run task every 1 second

        try {
            Thread.sleep(completionTime*1000); //end timer after completion time
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        timer.cancel(); //all processes done
        System.out.printf("|"); 
    }

    //turnaroundTime & waitingTime: calculate turnaround and waiting time
    private int turnaroundTime(int cT, int aT) { return cT - aT; }
    private int waitingTime(int tAT, int bT) { return tAT - bT; }

    //print gantt chart borders
    private void printSeconds (int completionTime) {
        for (int i=0; i<=completionTime; i++) {
            System.out.printf(""+i+"\t");
        } System.out.println();
    }
    private void printTopBottom (int completionTime) {
        System.out.printf(" ");
        for (int i=0; i<completionTime; i++) {
            System.out.printf("--------");
        } System.out.println();
    }

    //schedule: main function
    public void schedule(ArrayList<Integer> p, ArrayList<Integer> burstTime, ArrayList<Integer> arrivalTime, ArrayList<Integer> priority) {
        process process[] = new process[p.size()]; int completionTime=0;
        
        //store processes to array
        for (int i=0; i<p.size() && i<10; i++) {
            int burst = burstTime.get(i);
            if (min(25, burst) == 0) { burst = ThreadLocalRandom.current().nextInt(25); }

            process[i] = new process(p.get(i), min(25, burst), min(25, arrivalTime.get(i)), min(10, priority.get(i)));
            completionTime += burstTime.get(i);
        } completionTime += process[0].aT; //in case arrival time doesnt start at 0

        //print time in seconds; print top part of gantt chart
        System.out.println("GANTT CHART: (If needed, adjust window size and/or zoom out to get a straight gantt chart)");
        printSeconds(completionTime);
        printTopBottom(completionTime);

        //check if preemptive or not
        boolean isNotPreemptive = Arrays.stream( arrivalTime.toArray() ).allMatch( t -> t==arrivalTime.toArray()[0] ); //check if all elements match
        if (!isNotPreemptive) { //if preemptive
            Arrays.sort(process, new sortByArrival());
            startSchedule(process, completionTime, false);
        } else { //if nonpreemptive
            Arrays.sort(process, new sortByPriority());
            startSchedule(process, completionTime, true);
        }  

        System.out.println();
        printTopBottom(completionTime); //print bottom part of gantt chart

        //print summary
        System.out.println("\nSUMMARY: Priority Scheduling");
        System.out.printf("\tBurst Time\tArrival Time\tPriority\tCompletion Time\tWaiting Time\tTurnaround Time\n");
        Arrays.sort(process, new sortByProcessID()); int sumTAT=0, sumWAT=0;
        for (int i=0; i<process.length; i++) {
            int cT = process[i].getCompletionTime();
            int tAT = turnaroundTime(cT, arrivalTime.get(i)); int wAT = waitingTime(tAT, burstTime.get(i));
            sumTAT += tAT; sumWAT += wAT;

            System.out.printf("P" +process[i].p+ "\t" +burstTime.get(i)+ "\t\t" +arrivalTime.get(i)+ "\t\t" +priority.get(i)+ "\t\t" +cT+ "\t\t" +wAT+ "\t\t" +tAT+ "\n");
        }
        float avgTAT = (float)sumTAT/process.length; float avgWAT = (float)sumWAT/process.length;
        System.out.printf("\nAverage Waiting Time: "+avgWAT+"s\nAverage Turnaround Time: "+avgTAT+"s\n");
    }
}
