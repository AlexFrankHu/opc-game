package com.lastbastion.app.iogame;

import com.iohao.game.action.skeleton.core.BarSkeleton;
import com.iohao.game.action.skeleton.core.BarSkeletonBuilder;
import com.iohao.game.bolt.broker.client.AbstractBrokerClientStartup;
import com.iohao.game.bolt.broker.core.client.BrokerClient;
import com.iohao.game.bolt.broker.core.client.BrokerClientBuilder;
import com.iohao.game.bolt.broker.core.client.BrokerClientType;
import com.lastbastion.app.iogame.action.ArenaCmdAction;
import com.lastbastion.app.iogame.action.BattlePassCmdAction;
import com.lastbastion.app.iogame.action.OnboardingCmdAction;
import com.lastbastion.app.iogame.action.SurvivorCmdAction;
import com.lastbastion.app.iogame.action.UserCmdAction;
import com.lastbastion.app.iogame.action.ZoneCmdAction;

/**
 * ioGame 逻辑服启动器：注册 {@link com.iohao.game.action.skeleton.annotation.ActionController}
 * 到 BarSkeleton，由 BrokerClient 连接到 BrokerServer。
 *
 * <p>BarSkeleton 描述「业务路由 + 拦截器 + 序列化」等骨架信息，
 * 逻辑服收到网关分发的请求后，由 BarSkeleton 执行对应的 Action 方法。</p>
 */
public final class GameLogicStartup extends AbstractBrokerClientStartup {

    @Override
    public BarSkeleton createBarSkeleton() {
        BarSkeletonBuilder b = BarSkeleton.newBuilder();
        // 关闭源文件 JavaDoc 解析：
        //   1) Surefire 运行时 classpath 上不含 src/main/java，qdox 找不到源文件会抛 NPE；
        //   2) 生产镜像打包同理不携带 .java；Doc 生成对运行时路由不必需。
        b.getSetting().setParseDoc(false);
        b.getSetting().setGenerateDoc(false);
        b.addActionController(UserCmdAction.class);
        b.addActionController(SurvivorCmdAction.class);
        b.addActionController(ZoneCmdAction.class);
        b.addActionController(ArenaCmdAction.class);
        b.addActionController(BattlePassCmdAction.class);
        b.addActionController(OnboardingCmdAction.class);
        return b.build();
    }

    @Override
    public BrokerClientBuilder createBrokerClientBuilder() {
        return BrokerClient.newBuilder()
                .appName("LastBastionLogic")
                .brokerClientType(BrokerClientType.LOGIC);
    }
}
