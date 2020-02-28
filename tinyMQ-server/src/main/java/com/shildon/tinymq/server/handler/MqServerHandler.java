package com.shildon.tinymq.server.handler;

import com.shildon.tinymq.core.MqRequest;
import com.shildon.tinymq.core.MqResponse;
import com.shildon.tinymq.storage.RedisTemplate;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 服务器端处理器。
 *
 * @author shildon<shildondu @ gmail.com>
 * @date May 6, 2016
 */
public class MqServerHandler extends SimpleChannelInboundHandler<MqRequest> {

	private final RedisTemplate redisTemplate = RedisTemplate.getInstance();

	private static final Logger LOGGER = LoggerFactory.getLogger(MqServerHandler.class);

	@Override
	public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
		LOGGER.error("server handler error!", cause);
		ctx.close();
	}

	private MqResponse handle(final MqRequest mqRequest) {
		final MqResponse mqResponse = new MqResponse()
				.setMqTransferType(mqRequest.getMqTransferType());

		final String queueId;
		final Object content;

		switch (mqRequest.getMqTransferType()) {
			case offer:
				queueId = mqRequest.getQueueId();
				content = mqRequest.getContent();
				final boolean result = this.redisTemplate.offer(queueId, content);
				mqResponse.setContent(result);
				break;

			case poll:
				queueId = mqRequest.getQueueId();
				content = this.redisTemplate.poll(queueId);
				mqResponse.setContent(content);
				break;

			default:
				break;
		}
		return mqResponse;
	}

	@Override
	protected void channelRead0(final ChannelHandlerContext channelHandlerContext, final MqRequest mqRequest) throws Exception {
		LOGGER.debug("enter server handler ...");
		LOGGER.debug("mq request type: " + mqRequest.getMqTransferType());
		if (null != mqRequest.getMqTransferType()) {
			final MqResponse mqResponse = this.handle(mqRequest);
			channelHandlerContext.writeAndFlush(mqResponse).addListener(ChannelFutureListener.CLOSE);
		} else {
			LOGGER.debug("mq request is null!");
		}
	}

}