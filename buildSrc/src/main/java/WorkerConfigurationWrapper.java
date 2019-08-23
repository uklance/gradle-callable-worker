import java.io.File;

import org.gradle.api.Action;
import org.gradle.process.JavaForkOptions;
import org.gradle.workers.ForkMode;
import org.gradle.workers.IsolationMode;
import org.gradle.workers.WorkerConfiguration;

public class WorkerConfigurationWrapper implements WorkerConfiguration {
	private final WorkerConfiguration delegate;

	public WorkerConfigurationWrapper(WorkerConfiguration delegate) {
		super();
		this.delegate = delegate;
	}

	@Override
	public void classpath(Iterable<File> arg0) {
		delegate.classpath(arg0);
	}

	@Override
	public void forkOptions(Action<? super JavaForkOptions> arg0) {
		delegate.forkOptions(arg0);
	}

	@Override
	public Iterable<File> getClasspath() {
		return delegate.getClasspath();
	}

	@Override
	public String getDisplayName() {
		return delegate.getDisplayName();
	}

	@Override
	public ForkMode getForkMode() {
		return delegate.getForkMode();
	}

	@Override
	public JavaForkOptions getForkOptions() {
		return delegate.getForkOptions();
	}

	@Override
	public IsolationMode getIsolationMode() {
		return delegate.getIsolationMode();
	}

	@Override
	public Object[] getParams() {
		return delegate.getParams();
	}

	@Override
	public void params(Object... arg0) {
		delegate.params(arg0);
	}

	@Override
	public void setClasspath(Iterable<File> arg0) {
		delegate.setClasspath(arg0);
	}

	@Override
	public void setDisplayName(String arg0) {
		delegate.setDisplayName(arg0);
	}

	@Override
	public void setForkMode(ForkMode arg0) {
		delegate.setForkMode(arg0);
	}

	@Override
	public void setIsolationMode(IsolationMode arg0) {
		delegate.setIsolationMode(arg0);
	}

	@Override
	public void setParams(Object... arg0) {
		delegate.setParams(arg0);
	}
}
