package milo.utils.impl;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.transaction.Transactional;
import java.io.File;
import java.util.logging.Logger;

@Transactional(Transactional.TxType.SUPPORTS)
public class BerkeleyDBFactory {

	private static final Logger LOG = Logger.getLogger(BerkeleyDBFactory.class.getName());

	private Environment environment = null;
	private Database database = null;

	private final String pathToBerkeleyData;
	private final String berkeleyDatabaseName;

	public BerkeleyDBFactory(String pathToBerkeleyData, String berkeleyDatabaseName) {
		this.pathToBerkeleyData = pathToBerkeleyData;
		this.berkeleyDatabaseName = berkeleyDatabaseName;
	}

	@PostConstruct
	private void openBerkeley() {
        if (pathToBerkeleyData != null && berkeleyDatabaseName != null && environment == null && database == null) {
            EnvironmentConfig envConfig = new EnvironmentConfig();
            envConfig.setAllowCreate(true);
            envConfig.setSharedCache(true);
            environment = new Environment(new File(pathToBerkeleyData), envConfig);
            DatabaseConfig dbConfig = new DatabaseConfig();
            dbConfig.setAllowCreate(true);
            database = environment.openDatabase(null, berkeleyDatabaseName, dbConfig);
        }
	}

	@PreDestroy
	private void closeBerkeley() {
        if (database != null) {
            database.close();
            database = null;
        }
        if (environment != null) {
            environment.close();
            environment = null;
        }
    }

    public Database getDatabase() {
        return database;
    }

}
