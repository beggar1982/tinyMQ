package com.shildon.tinymq.client.subscriber;

import com.shildon.tinymq.client.MessageClient;
import com.shildon.tinymq.client.RegistryConsumerTable;
import com.shildon.tinymq.core.protocol.*;
import com.shildon.tinymq.core.serializer.ProtostuffSerializer;
import com.shildon.tinymq.core.serializer.Serializer;
import com.shildon.tinymq.core.util.MessageIdUtils;
import io.netty.channel.Channel;

import java.util.function.Consumer;

/**
 * @author shildon
 */
public class Subscriber<T> {

    private MessageClient messageClient = MessageClient.getInstance();
    private RegistryConsumerTable registryConsumerTable = RegistryConsumerTable.getInstance();
    private Serializer defaultSerializer = new ProtostuffSerializer();
    private Serializer serializer;
    private Class<T> genericType;

    Subscriber(Class<T> genericType, Serializer serializer) {
        this.serializer = serializer;
        this.genericType = genericType;
    }

    public void subscribe(String topic, Consumer<T> consumer) throws Exception {
        Channel channel = messageClient.borrowChannel();
        try {
            MessageHeader header = new MessageHeader();
            header.setMessageId(MessageIdUtils.generate());
            header.setMessageType(MessageType.SUBSCRIBE.getValue());
            // todo use configuration of group
            SubscribeMessageBody subscribeMessageBody = new SubscribeMessageBody(topic, "");
            byte[] serializedData = this.defaultSerializer.serialize(subscribeMessageBody);
            MessageBody body = new MessageBody(serializedData);
            MessageProtocol request = new MessageProtocol(header, body);
            channel.writeAndFlush(request);
            Consumer<PublishMessageBody> wrappedConsumer = responseBody -> {
                byte[] serializedMessage = responseBody.getSerializedMessage();
                T message = serializer.deserialize(serializedMessage, this.genericType);
                consumer.accept(message);
            };
            registryConsumerTable.put(topic, wrappedConsumer);
        } finally {
            messageClient.returnChannel(channel);
        }
    }

}
