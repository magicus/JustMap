package ru.bulldog.justmap.util.tasks;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;

import ru.bulldog.justmap.JustMap;

public class TaskManager implements Executor {
    private final Queue<Task> workQueue = new ConcurrentLinkedQueue<>();
    private final QueueBlocker queueBlocker;
    private Thread worker;   
    private String name = JustMap.MODID;
    private boolean running = true; 
    
    private static Map<String, TaskManager> managers = new HashMap<>();
    
    public static TaskManager getManager(String name) {
    	if (managers.containsKey(name)) {
    		TaskManager manager = managers.get(name);
    		if (!manager.isRunning()) {
    			manager = new TaskManager(name);
    			managers.replace(name, manager);
    		}
    		
    		return manager;
    	}
    	
    	TaskManager manager = new TaskManager(name);
    	managers.put(name, manager);
    	
    	return manager;
    }
    
    public static void shutdown() {
    	managers.forEach((name, manager) -> {
    		if (manager.isRunning()) manager.stop();
    	});
    }
    
    private TaskManager(String name) {
    	this.name += "-" + name;
    	this.queueBlocker = new QueueBlocker(this.name + "-blocker");
    	this.worker = new Thread(this::work, this.name);
    	this.worker.start();
    }
    
    public void execute(String reason, Runnable command) {
    	this.workQueue.offer(new Task(reason, command));
    	LockSupport.unpark(this.worker);
    }

    @Override
    public void execute(Runnable command) {
    	this.execute("Common task", command);
    }
    
    public <T> CompletableFuture<T> run(Function<CompletableFuture<T>, Runnable> function) {
		return this.run("Future task", function);
	}
    
    public <T> CompletableFuture<T> run(String reason, Function<CompletableFuture<T>, Runnable> function) {
    	CompletableFuture<T> completableFuture = new CompletableFuture<>();
    	this.execute(reason, function.apply(completableFuture));
    	return completableFuture;
    }
    
    public void stop() {
    	this.execute("Stopping " + this.name, () -> {
    		this.running = false;
    	});    	
    }
    
    public int queueSize() {
    	return this.workQueue.size();
    }
    
    public boolean isRunning() {
    	return this.running;
    }

    private void work() {
    	while (running) {
    		Task nextTask = workQueue.poll();
    		if (nextTask != null) {
    			JustMap.LOGGER.debug(nextTask);
    			nextTask.run();
            } else {
            	LockSupport.park(queueBlocker);
            }
        }
    }
    
    private class Task implements Runnable {
    	
    	private final Runnable task;
    	private final String reason;
    	
    	private Task(String reason, Runnable task) {
    		this.reason = reason;
    		this.task = task;
    	}
    	
    	@Override
		public void run() {
			this.task.run();
		}
    	
    	public String getReason() {
    		return this.reason;
    	}
    	
    	@Override
    	public String toString() {
    		return this.getReason();
    	}
    }
    
    private class QueueBlocker {
    	private final String name;
    	
    	private QueueBlocker(String name) {
    		this.name = name;
    	}
    	
    	@Override
    	public String toString() {
    		return this.name;
    	}
    }
}