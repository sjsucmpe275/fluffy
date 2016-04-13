package gash.router.container;

import java.io.File;

public interface Subject {

	public void registerObserver(Observer observer);

	public void removeObserver(Observer observer);

	public void notifyObservers(File file);

}
