package org.apache.dubbo.admin.common.util;

/**
 * The Four Letter Words
 * https://zookeeper.apache.org/doc/r3.5.5/zookeeperAdmin.html#sc_zkCommands
 *
 * @author heyudev
 * @date 2019/05/31
 */
public class FourLetterWords {
    /**
     * conf命令用于输出zookeeper服务器运行时使用的基本配置信息，包括clientPort,dataDir和tickTIme等
     * New in 3.3.0: Print details about serving configuration.
     */
    public static final String CONF = "conf";
    /**
     * 用于输出当前这台服务器上所有客户端连接的详细信息，包括每个客户端的IP，会话ID和最后一次与服务器交互的操作类型等
     * New in 3.3.0: List full connection/session details for all clients connected to this server. Includes information on numbers of packets received/sent, session id, operation latencies, last operation performed, etc...
     */
    public static final String CONS = "cons";
    /**
     * 用于重置所有的客户端连接统计信息
     * New in 3.3.0: Reset connection/session statistics for all connections.
     */
    public static final String CRST = "crst";
    /**
     * 用于输出当前集群的所有会话信息，包括这些会话的ID及创建的临时节点等信息
     * Lists the outstanding sessions and ephemeral nodes. This only works on the leader.
     */
    public static final String DUMP = "dump";
    /**
     * 用于输出当前集群的所有会话信息，包括这些会话的ID及创建的临时节点等信息
     * Print details about serving environment
     */
    public static final String ENVI = "envi";
    /**
     * 用于输出当前zookeeper服务器是否正在运行
     * Tests if server is running in a non-error state. The server will respond with imok if it is running. Otherwise it will not respond at all. A response of "imok" does not necessarily indicate that the server has joined the quorum, just that the server process is active and bound to the specified client port. Use "stat" for details on state wrt quorum and client connection information.
     */
    public static final String RUOK = "ruok";
    /**
     * 用于重置所有服务器的统计信息
     * Reset server statistics.
     */
    public static final String SRST = "srst";
    /**
     * 和stat功能一致，唯一的区别是srvr不会将客户端的连接情况输出，仅仅输出服务器的自身信息
     * New in 3.3.0: Lists full details for the server.
     */
    public static final String SRVR = "srvr";
    /**
     * 用于获取服务器的运行状态信息，包括基本的zookeeper版本、打包信息、运行时角色、集群数据节点个数等信息，另外还会将当前服务器的客户端连接信息打印出来
     * Lists brief details for the server and connected clients.
     */
    public static final String STAT = "stat";
    /**
     * 用于输出当前服务器上管理的watcher的概要信息
     * New in 3.3.0: Lists brief information on watches for the server.
     */
    public static final String WCHS = "wchs";
    /**
     * 用于输出当前服务器上管理的watcher的详细信息，以会话为单位进行归组，同时列出被该会话注册了watcher的节点路径 (谨慎使用)
     * New in 3.3.0: Lists detailed information on watches for the server, by session. This outputs a list of sessions(connections) with associated watches (paths).
     * Note, depending on the number of watches this operation may be expensive (ie impact server performance), use it carefully.
     */
    public static final String WCHC = "wchc";
    /**
     * New in 3.5.1: Shows the total size of snapshot and log files in bytes
     */
    public static final String DIRS = "dirs";
    /**
     * (谨慎使用)
     * New in 3.3.0: Lists detailed information on watches for the server, by path. This outputs a list of paths (znodes) with associated sessions.
     * Note, depending on the number of watches this operation may be expensive (ie impact server performance), use it carefully.
     */
    public static final String WCHP = "wchp";
    /**
     * 用于输出比stat命令更为详尽的服务器统计信息，包括请求处理的延迟情况，服务器内存数据库大小和集群的数据同步情况
     * New in 3.4.0: Outputs a list of variables that could be used for monitoring the health of the cluster.
     */
    public static final String MNTR = "mntr";
    /**
     * New in 3.4.0: Tests if server is running in read-only mode. The server will respond with "ro" if in read-only mode or "rw" if not in read-only mode.
     */
    public static final String ISRO = "isro";
    /**
     * Gets the current trace mask as a 64-bit signed long value in decimal format. See stmk for an explanation of the possible values.
     */
    public static final String GTMK = "gtmk";
    /**
     * Sets the current trace mask. The trace mask is 64 bits, where each bit enables or disables a specific category of trace logging on the server. Log4J must be configured to enable TRACE level first in order to see trace logging messages. The bits of the trace mask correspond to the following trace logging categories.
     */
    public static final String STMK = "stmk";
}
