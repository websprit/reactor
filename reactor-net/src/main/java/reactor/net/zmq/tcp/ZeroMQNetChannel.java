package reactor.net.zmq.tcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;
import reactor.core.Environment;
import reactor.core.Reactor;
import reactor.core.composable.Deferred;
import reactor.core.composable.Promise;
import reactor.core.spec.Reactors;
import reactor.event.dispatch.Dispatcher;
import reactor.function.Consumer;
import reactor.io.Buffer;
import reactor.io.encoding.Codec;
import reactor.net.AbstractNetChannel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Queue;

/**
 * @author Jon Brisbin
 */
public class ZeroMQNetChannel<IN, OUT> extends AbstractNetChannel<IN, OUT> {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private volatile String      connectionId;
	private volatile ZMQ.Socket  socket;
	private volatile Queue<ZMsg> outputQueue;

	public ZeroMQNetChannel(@Nonnull Environment env,
	                        @Nonnull Reactor eventsReactor,
	                        @Nonnull Dispatcher ioDispatcher,
	                        @Nullable Codec<Buffer, IN, OUT> codec) {
		super(env, codec, ioDispatcher, eventsReactor);
	}

	public ZeroMQNetChannel<IN, OUT> setConnectionId(String connectionId) {
		this.connectionId = connectionId;
		return this;
	}

	public ZeroMQNetChannel<IN, OUT> setSocket(ZMQ.Socket socket) {
		this.socket = socket;
		return this;
	}

	public ZeroMQNetChannel<IN, OUT> setOutputQueue(Queue<ZMsg> outputQueue) {
		this.outputQueue = outputQueue;
		return this;
	}

	@Override
	protected void write(ByteBuffer data, Deferred<Void, Promise<Void>> onComplete, boolean flush) {
		byte[] bytes = new byte[data.remaining()];
		data.get(bytes);
		final ZMsg msg = new ZMsg();
		msg.add(new ZFrame(connectionId));
		msg.add(new ZFrame(bytes));
		outputQueue.add(msg);
		Reactors.schedule(onComplete, null, getEventsReactor());
	}

	@Override
	protected void write(Object data, Deferred<Void, Promise<Void>> onComplete, boolean flush) {
		log.info("data: {}", data);
		Reactors.schedule(onComplete, null, getEventsReactor());
	}

	@Override
	protected void flush() {

	}

	@Override
	public void close(Consumer<Void> onClose) {
		Reactors.schedule(onClose, null, getEventsReactor());
	}

	@Override
	public ConsumerSpec on() {
		return new ZeroMQConsumerSpec();
	}

	private class ZeroMQConsumerSpec implements ConsumerSpec {
		@Override
		public ConsumerSpec close(Runnable onClose) {

			return null;
		}

		@Override
		public ConsumerSpec readIdle(long idleTimeout, Runnable onReadIdle) {
			return null;
		}

		@Override
		public ConsumerSpec writeIdle(long idleTimeout, Runnable onWriteIdle) {
			return null;
		}
	}

}