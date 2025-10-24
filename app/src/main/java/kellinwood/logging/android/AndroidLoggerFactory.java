package kellinwood.logging.android;


import kellinwood.logging.Logger;
import kellinwood.logging.LoggerFactory;
import pro.sketchware.utility.TranslationFunction;

public class AndroidLoggerFactory implements LoggerFactory
{
    protected static AndroidLoggerFactory instance = new AndroidLoggerFactory();

    private AndroidLoggerFactory() {}

    public static AndroidLoggerFactory getInstance() {
        return instance;
    }

	@Override
	public Logger getLogger(String category) {
		return new AndroidLogger( category);
	}

}
