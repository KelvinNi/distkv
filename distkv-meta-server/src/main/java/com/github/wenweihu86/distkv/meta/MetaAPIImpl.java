package com.github.wenweihu86.distkv.meta;

import com.github.wenweihu86.distkv.api.CommonMessage;
import com.github.wenweihu86.distkv.api.MetaAPI;
import com.github.wenweihu86.distkv.api.MetaMessage;
import com.github.wenweihu86.raft.RaftNode;
import com.github.wenweihu86.raft.proto.RaftMessage;
import com.github.wenweihu86.rpc.client.RPCClient;
import com.github.wenweihu86.rpc.client.RPCProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by wenweihu86 on 2017/6/8.
 */
public class MetaAPIImpl implements MetaAPI {

    private static final Logger LOG = LoggerFactory.getLogger(MetaAPIImpl.class);

    private RaftNode raftNode;
    private MetaStateMachine stateMachine;

    public MetaAPIImpl(RaftNode raftNode, MetaStateMachine stateMachine) {
        this.raftNode = raftNode;
        this.stateMachine = stateMachine;
    }

    @Override
    public MetaMessage.SetResponse set(MetaMessage.SetRequest request) {
        MetaMessage.SetResponse.Builder responseBuilder = MetaMessage.SetResponse.newBuilder();
        CommonMessage.BaseResponse.Builder baseResBuilder = CommonMessage.BaseResponse.newBuilder();
        // 如果自己不是leader，将写请求转发给leader
        if (raftNode.getLeaderId() <= 0) {
            baseResBuilder.setResCode(CommonMessage.ResCode.RES_CODE_FAIL);
            baseResBuilder.setResMsg("leader not exists");
            responseBuilder.setBaseRes(baseResBuilder);
        } else if (raftNode.getLeaderId() != raftNode.getLocalServer().getServerId()) {
            RPCClient rpcClient = raftNode.getPeerMap().get(raftNode.getLeaderId()).getRpcClient();
            MetaAPI storeAPI = RPCProxy.getProxy(rpcClient, MetaAPI.class);
            MetaMessage.SetResponse responseFromLeader = storeAPI.set(request);
            responseBuilder.mergeFrom(responseFromLeader);
        } else {
            // 数据同步写入raft集群
            byte[] data = request.toByteArray();
            boolean success = raftNode.replicate(data, RaftMessage.EntryType.ENTRY_TYPE_DATA);
            baseResBuilder.setResCode(
                    success ? CommonMessage.ResCode.RES_CODE_SUCCESS
                            : CommonMessage.ResCode.RES_CODE_FAIL);
            responseBuilder.setBaseRes(baseResBuilder);
        }

        MetaMessage.SetResponse response = responseBuilder.build();
        LOG.info("set request, request={}, response={}",
                ProtobufUtils.protoToJson(request),
                ProtobufUtils.protoToJson(response));
        return response;
    }

    @Override
    public MetaMessage.GetResponse get(MetaMessage.GetRequest request) {
        MetaMessage.GetResponse response = stateMachine.get(request);
        LOG.info("get request, request={}, response={}",
                ProtobufUtils.protoToJson(request),
                ProtobufUtils.protoToJson(response));
        return response;
    }

    @Override
    public MetaMessage.DeleteResponse delete(MetaMessage.DeleteRequest request) {
        MetaMessage.DeleteResponse.Builder responseBuilder = MetaMessage.DeleteResponse.newBuilder();
        CommonMessage.BaseResponse.Builder baseResBuilder = CommonMessage.BaseResponse.newBuilder();
        // 如果自己不是leader，将写请求转发给leader
        if (raftNode.getLeaderId() <= 0) {
            baseResBuilder.setResCode(CommonMessage.ResCode.RES_CODE_FAIL);
            baseResBuilder.setResMsg("leader not exists");
            responseBuilder.setBaseRes(baseResBuilder);
        } else if (raftNode.getLeaderId() != raftNode.getLocalServer().getServerId()) {
            RPCClient rpcClient = raftNode.getPeerMap().get(raftNode.getLeaderId()).getRpcClient();
            MetaAPI storeAPI = RPCProxy.getProxy(rpcClient, MetaAPI.class);
            MetaMessage.DeleteResponse responseFromLeader = storeAPI.delete(request);
            responseBuilder.mergeFrom(responseFromLeader);
        } else {
            // 数据同步写入raft集群
            byte[] data = request.toByteArray();
            boolean success = raftNode.replicate(data, RaftMessage.EntryType.ENTRY_TYPE_DATA);
            baseResBuilder.setResCode(
                    success ? CommonMessage.ResCode.RES_CODE_SUCCESS
                            : CommonMessage.ResCode.RES_CODE_FAIL);
            responseBuilder.setBaseRes(baseResBuilder);
        }

        MetaMessage.DeleteResponse response = responseBuilder.build();
        LOG.info("delete request, request={}, response={}",
                ProtobufUtils.protoToJson(request),
                ProtobufUtils.protoToJson(response));
        return response;
    }

}
