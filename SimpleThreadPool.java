import java.util.LinkedList;
import java.util.List;

public class SimpleThreadPool {

    private final int nThreads; // 线程池中的线程数量
    private final List<Worker> workers; // 保存工作线程的列表
    private final LinkedList<Runnable> taskQueue; // 任务队列
    private volatile boolean isShutdown = false; // 线程池是否已关闭

    public SimpleThreadPool(int nThreads) {
        this.nThreads = nThreads;
        taskQueue = new LinkedList<>();
        workers = new LinkedList<>();

        // 创建并启动工作线程
        for (int i = 0; i < nThreads; i++) {
            Worker worker = new Worker();
            workers.add(worker);
            new Thread(worker, "Worker-" + i).start();
        }
    }

    // 提交任务到线程池
    public synchronized void execute(Runnable task) {
        if (!isShutdown) {
            taskQueue.add(task);
            notify(); // 通知等待的工作线程有新任务可执行
        }
    }

    // 关闭线程池，等待所有任务完成
    public synchronized void shutdown() {
        isShutdown = true;
        for (Worker worker : workers) {
            worker.interrupt(); // 中断所有工作线程
        }
    }

    // 工作线程类
    private class Worker implements Runnable {

        @Override
        public void run() {
            Runnable task;
            while (true) {
                synchronized (SimpleThreadPool.this) {
                    // 如果任务队列为空且未关闭，则等待
                    while (taskQueue.isEmpty() && !isShutdown) {
                        try {
                            SimpleThreadPool.this.wait();
                        } catch (InterruptedException e) {
                            // 收到中断信号，退出循环
                            return;
                        }
                    }

                    // 如果线程池已关闭且任务队列为空，退出
                    if (isShutdown && taskQueue.isEmpty()) {
                        return;
                    }

                    // 从任务队列中取出一个任务
                    task = taskQueue.removeFirst();
                }

                // 执行任务
                try {
                    task.run();
                } catch (RuntimeException e) {
                    // 捕获任务中的异常，避免线程退出
                    System.err.println("Task execution failed: " + e.getMessage());
                }
            }
        }

        public void interrupt() {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        SimpleThreadPool threadPool = new SimpleThreadPool(3); // 创建一个包含3个线程的线程池

        // 提交5个任务到线程池
        for (int i = 1; i <= 4; i++) {
            int taskNumber = i;
            threadPool.execute(() -> {
                System.out.println("Task " + taskNumber + " is running in thread: " + Thread.currentThread().getName());
                try {
                    Thread.sleep(20000); // 模拟任务执行时间
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("Task " + taskNumber + " finished in thread: " + Thread.currentThread().getName());
            });
        }

        // 关闭线程池
        threadPool.shutdown();
    }
}
