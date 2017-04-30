# Airport_Simulation
CSE6730 project. Object oriented programming applied to the discrete event simulation.

non-blocking思路：
1.non-blocking（非阻塞）的信息传输的实现方式：对于某个processor i，在任何时候只保留最多一个来自相同processor的非阻塞请求（req = MPI.Irecv(...)），也就是说，完成一次接收才开始下一次接收。因为根据mpi，只要保证了发送端发送信息的顺序，接收就一定能按照发送的顺序来接收，所以每个循环只需要进行一次接收就可以了，等接收完成了再进行下一次接收。

2.判断一个non-blocking receive是否接收完成需要用到下面的三个mpi.request的method。
  
  Wait():等待非阻塞接收／发送的完成。
  Test():调用Is_Null()之前要先进行Test(),除非之前已经调用过Wait()。
  Is_Null():检查非阻塞接收／发送是否完成。返回：true完成，false未完成。

Null-Message 步骤：
1.遍历检查req[i]是否完成
  （1）如果req[i]==null，说明是程序刚开始运行，还没进行过non-blocking receive.
  （2）通过test和isnull判断是否完成，如果req还没完成，继续等待。
  （3）如果已经完成，这里有两种情况：
        a）在上一个循环已经进行了wait()，所以接收的信息已经用来新建过event了，可以直接进行下一个非阻塞接收。
        b）通过test和isnull判断已经完成了，通过接收的信息来新建一个event存到incomingQueue里，再进行下一个非阻塞接收。

2.遍历检查有没有接收queue为空（即判断queuecount[i]是否为0）
  （1）若为零，对所有processors都发一条null message，然后等待空的queue接收完成。
  （2）若不为零，则把lbts之前时间都执行了。
  
3.某个LP运行到stop-time，对其他LP发送结束标记，避免还没结束的LP进入deadlock。
