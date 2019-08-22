import org.gradle.api.*;
import org.gradle.api.file.*;
import org.gradle.api.provider.*;
import org.gradle.api.tasks.*;
import org.gradle.workers.*;
import java.lang.reflect.*;
import java.util.*;
import java.io.*;
import java.util.concurrent.*;
import javax.inject.*;

public class CallableWorkerExecutor<T> {
	private final File resultDir;
	private final WorkerExecutor workerExecutor; 
	private final List<File> workerFiles;

    public CallableWorkerExecutor(File resultDir, WorkerExecutor workerExecutor) {
		this.resultDir = resultDir;
		this.workerExecutor = workerExecutor;
		this.workerFiles = new ArrayList<>();
	}
	
	public synchronized void submit(Class<? extends Callable<T>> actionClass, Action<? super CallableWorkerConfiguration> configAction) {
		resultDir.mkdirs();
		File workerFile = new File(resultDir, workerFiles.size() + ".obj");
		workerFiles.add(workerFile);
		CallableWorkerConfiguration config = new CallableWorkerConfiguration();
		configAction.execute(config);
		List<Object> paramsList = config.getParams() == null ? Collections.emptyList() : Arrays.asList(config.getParams());
		Object[] wrappedParams = { actionClass.getName(), workerFile, paramsList };
		workerExecutor.submit(CallableWrapper.class, (wrappedConfig) -> {
			wrappedConfig.setParams(wrappedParams);
		});
	}
	
	public synchronized List<T> await() {
		workerExecutor.await();
		List results = new ArrayList();
		for (File workerFile : workerFiles) {
			try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(workerFile))) {
				results.add(in.readObject());
				workerFile.delete();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return results;
	}
	
	public static class CallableWrapper implements Runnable {
		private final Callable callable;
		private final File workerFile;
		
		@Inject
		public CallableWrapper(String typeName, File workerFile, List<Object> args) throws Exception {
			Class type = Class.forName(typeName);
			Constructor[] constructors = type.getConstructors();
			if (constructors.length != 1) {
				throw new RuntimeException(String.format("Expected 1 constructor for type %s found %s", type.getName(), constructors.length));
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
	
	public static class CallableWorkerConfiguration {
		private Object[] params;
		
		public void params(Object... params) {
			this.params = params;
		}
		
		public Object[] getParams() {
			return this.params;
		}
	}
}