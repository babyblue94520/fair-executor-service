# Fair ExecutorService

## Overview

This is an __ExecutorService__ of multiple __queue__ structures, which handles the tasks in each __queue__ fairly.
Each __queue__ will only deliver a fixed amount to the shared __ExecutorService__ at the same time. The next task will
not be delivered until the task is completed, which is different from the traditional The __ExecutorService__ may be
delivered a large number of tasks by specific business logic, blocking all thread.

## QuickStart

### pom.xml

```xml

<dependency>
    <groupId>io.github.babyblue94520</groupId>
    <artifactId>fair-executor-service</artifactId>
    <version>1.0.0-RELEASE</version>
</dependency>
```

## Usage

* __Simple__

    ```java
    class Example {
        
        public static void main(String[] args) {
            FairExecutorService<Object> executorService = new FairExecutorService<>();
            Object key = new Object();
            executorService.execute(key,()->{
                // TODO
            });
        }
    }
    ```

* __Concurrent__

  The number of tasks that each queue can execute concurrently.

    ```java
    class Example {
        
        public static void main(String[] args) {
            FairExecutorService<Object> executorService = new FairExecutorService<>(2);
            Object key = new Object();
            executorService.execute(key,()->{
                // TODO
            });
        }
    }
    ```
    
* __ExecutorService__

  Custom ExecutorService.

    ```java
    class Example {
        
        public static void main(String[] args) {
            FairExecutorService<Object> executorService = new FairExecutorService<>(Executors.newCachedThreadPool());
            Object key = new Object();
            executorService.execute(key,()->{
                // TODO
            });
        }
    }
    ```

## Test

Slow task will not block other fast tasks.

```text
thread count: 16
key count: 20, task: 100, total task: 2000
slow key count: 4, run time: 2 ms
nullKey: false, blocking: false
[slow] key: 0 total waiting time: 10062 
[    ] key: 1 total waiting time: 400 
[    ] key: 2 total waiting time: 400 
[    ] key: 3 total waiting time: 400 
[    ] key: 4 total waiting time: 400 
[slow] key: 5 total waiting time: 10062 
[    ] key: 6 total waiting time: 400 
[    ] key: 7 total waiting time: 400 
[    ] key: 8 total waiting time: 400 
[    ] key: 9 total waiting time: 400 
[slow] key: 10 total waiting time: 10062 
[    ] key: 11 total waiting time: 401 
[    ] key: 12 total waiting time: 401 
[    ] key: 13 total waiting time: 401 
[    ] key: 14 total waiting time: 401 
[slow] key: 15 total waiting time: 10231 
[    ] key: 16 total waiting time: 403 
[    ] key: 17 total waiting time: 404 
[    ] key: 18 total waiting time: 404 
[    ] key: 19 total waiting time: 404 
```
