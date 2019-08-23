import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.workers.WorkerConfiguration;
import org.gradle.workers.WorkerExecutor;

public class CallableWorkerExecutor {
	private final File resultDir;
	private final WorkerExecutor workerExecutor;
	private final List<File> workerFiles;

	public CallableWorkerExecutor(File resultDir, WorkerExecutor workerExecutor) {
		this.resultDir = resultDir;
		this.workerExecutor = workerExecutor;
		this.workerFiles = new ArrayList<>();
	}

	public synchronized void submit(Class<? extends Callable<?>> actionClass, Action<? super WorkerConfiguration> configAction) {
		resultDir.mkdirs();
		File workerFile = new File(resultDir, workerFiles.size() + ".obj");
		workerFiles.add(workerFile);
		workerExecutor.submit(CallableWrapper.class, (internalConfig) -> {
			CallableWorkerConfiguration wrappedConfig = new CallableWorkerConfiguration(internalConfig);
			configAction.execute(wrappedConfig);
			List<Object> paramsList = wrappedConfig.getParams() == null ? Collections.emptyList() : Arrays.asList(wrappedConfig.getParams());
			Object[] internalParams = { actionClass.getName(), workerFile, paramsList };
			internalConfig.setParams(internalParams);
		});
	}

	public synchronized List<Object> await() {
		workerExecutor.await();
		List<Object> results = new ArrayList<>();
		for (File workerFile : workerFiles) {
			try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(workerFile))) {
				results.add(in.readObject());
				workerFile.delete();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		workerFiles.clear();
		return results;
	}

	public static class CallableWrapper implements Runnable {
		private final Callable<?> callable;
		private final File workerFile;

		@Inject
		public CallableWrapper(String typeName, File workerFile, List<Object> args) throws Exception {
			Class<?> type = Class.forName(typeName);
			Constructor<?>[] constructors = type.getConstructors();
			if (constructors.length != 1) {
				throw new RuntimeException(String.format("Expected 1 constructor for type %s found %s", type.getName(),
						constructors.length));
			}
			this.callable = (Callable) constructors[0].newInstance(args.toArray());
			this.workerFile = workerFile;
		}

		public void run() {
			try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(workerFile))) {
				out.writeObject(callable.call());
				out.flush();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static class CallableWorkerConfiguration extends WorkerConfigurationWrapper {
		public CallableWorkerConfiguration(WorkerConfiguration delegate) {
			super(delegate);
		}

		private Object[] params;

		@Override
		public void params(Object... params) {
			setParams(params);
		}

		@Override
		public Object[] getParams() {
			return this.params;
		}
		
		@Override
		public void setParams(Object... params) {
			this.params = params;
		}
	}
}
