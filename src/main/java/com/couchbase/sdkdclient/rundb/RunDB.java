/**
 * Copyright 2013, Couchbase Inc.
 */
package com.couchbase.sdkdclient.rundb;
import com.couchbase.sdkdclient.options.OptionLookup;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.DatabaseConnection;
import com.j256.ormlite.table.TableUtils;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class provides the core interaction with the sdkdclient database.
 * To use this class, you will need to give it a path via {@link DatabasePath}.
 * Once a path has been set up, you may then instantiate a new connection either
 * for writing (via {@link #createWriter(DatabasePath)}) or reading (via
 * {@link #createReader(DatabasePath)}).
 */
public class RunDB {

  /**
   * Format of the database file. This specifies the type of database driver
   * to be used.
   */
  public enum Format {
    SQLITE,
    H2,
    GUESS
  };

  /**
   * Database version. Incremented each time a change to the table is made.
   */
  static public final int VERSION = 0x020000;
  private final JdbcConnectionSource connSrc;
  private final RunEntry runEnt;

  private final Dao<LogEntry, Void> daoLog;
  private final Dao<WorkloadEntry, Long> daoWorkload;
  private final Dao<HandleEntry, Long> daoHandle;
  private final Dao<RunEntry, UUID> daoRun;
  private final Dao<ConfigEntry, Void> daoConfig;
  private final Dao<MiscEntry, Void> daoMisc;
  private final Object lock = new Object();
  private final Format format;
  private volatile long idSerial = 500;

  static private final Logger logger = LoggerFactory.getLogger(RunDB.class);
  static private final String jdbcFormatBase = "jdbc:%s:%s%s";

  private <T extends DBEntry> int create(Dao<T, ?> dao, T data) throws SQLException {
    synchronized(lock) {
      data.setDbId(idSerial++);
      return dao.create(data);
    }
  }

  private <T> int refresh(Dao<T, ?> dao, T data) throws SQLException {
    synchronized(lock) {
      return dao.refresh(data);
    }
  }

  private void createTables() throws SQLException {
    TableUtils.createTable(connSrc, RunEntry.class);
    TableUtils.createTable(connSrc, LogEntry.class);
    TableUtils.createTable(connSrc, WorkloadEntry.class);
    TableUtils.createTable(connSrc, HandleEntry.class);
    TableUtils.createTable(connSrc, ConfigEntry.class);
    TableUtils.createTable(connSrc, MiscEntry.class);
  }

  public final void createDb() throws SQLException {
    createTables();
    daoRun.create(runEnt);
  }

  public final void loadDb() throws SQLException {
    if (daoRun.countOf() != 1) {
      logger.error("More than one RunEntry inside database");
    }
    daoRun.refresh(runEnt);
  }

  public synchronized void close() {
    try {
      if (format == Format.H2) {
        DatabaseConnection dbConn = connSrc.getReadWriteConnection();
        dbConn.executeStatement("SHUTDOWN",
                                DatabaseConnection.DEFAULT_RESULT_FLAGS);
      }
      connSrc.close();
    } catch (SQLException ex) {
      logger.debug("While closing", ex);
    }
  }

  /**
   * Each 'Run' is a parent of one or more 'Workloads', each 'Workload'
   * has one or more performance timings.
   */
  protected RunDB(String output, Format fmt, boolean isWriter) throws SQLException {
    String sDriver;
    format = fmt;
    String sPath = output;
    if (fmt == Format.H2) {
      sDriver = "h2";
    } else {
      sDriver = "sqlite";
    }

    String connStr = String.format(jdbcFormatBase, sDriver, sPath, "");
    logger.debug("Connecting to {}", connStr);
    if (format == Format.H2) {
      connStr += ";TRACE_LEVEL_FILE=0";

      if (!isWriter) {
        connStr += ";ACCESS_MODE_DATA=r";
      }

      logger.debug("Connection string modified: {}", connStr);
    }

    connSrc = new JdbcConnectionSource(connStr);

    daoRun = DaoManager.createDao(connSrc, (Class)RunEntry.class);
    daoLog = DaoManager.createDao(connSrc, (Class)LogEntry.class);
    daoHandle = DaoManager.createDao(connSrc, (Class)HandleEntry.class);
    daoWorkload = DaoManager.createDao(connSrc, (Class)WorkloadEntry.class);
    daoConfig = DaoManager.createDao(connSrc, (Class)ConfigEntry.class);
    daoMisc = DaoManager.createDao(connSrc, (Class)MiscEntry.class);


    if (fmt == Format.SQLITE) {
      connSrc.getReadWriteConnection().executeStatement(
              "PRAGMA synchronous=OFF;", DatabaseConnection.DEFAULT_RESULT_FLAGS);
    }

    if (isWriter) {
      createDb();
      runEnt = new RunEntry();
      daoRun.create(runEnt);

      // Set our version here!
      addProperty(MiscEntry.K_DBVERSION, ""+VERSION);

    } else {
      runEnt = daoRun.queryBuilder().queryForFirst();
      loadDb();
    }
  }

  /**
   * Creates a new RunDB object for writing data
   * @param pth The path object.
   * @return A new {@link RunDB} instance.
   * @throws SQLException
   */
  public static RunDB createWriter(DatabasePath pth) throws SQLException {
    return new RunDB(pth.getDbPath(), pth.getFormat(), true);
  }

  /**
   * Creates a new RunDB object for reading data.
   * @param pth The path object.
   * @return A new {@link RunDB} instance.
   * @throws SQLException
   */
  public static RunDB createReader(DatabasePath pth) throws SQLException {
    return new RunDB(pth.getDbPath(), pth.getFormat(), false);
  }

  //<editor-fold defaultstate="collapsed" desc="Writer Methods">
  public void addLogEntry(LogEntry logent) throws SQLException {
    create(daoLog, logent);
  }

  public void addConfig(OptionLookup lookup) throws SQLException {
    final List<ConfigEntry> entries = ConfigEntry.createEntries(runEnt, lookup);

    try {
      synchronized(lock) {
        daoConfig.callBatchTasks(new Callable<Boolean>() {
          @Override
          public Boolean call() throws SQLException {
            for (ConfigEntry ent : entries) {
              daoConfig.create(ent);
            }
            return true;
          }
        });
      }
    } catch (SQLException ex) {
      throw ex;

    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public final void addProperty(String key, String value) throws SQLException {
    MiscEntry ent = new MiscEntry(runEnt, key, value);
    create(daoMisc, ent);
  }

  public final void addPropertyQuietly(String key, String value) {
    try {
      addProperty(key, value);
    } catch (SQLException ex) {
      logger.error("While adding property " + key, ex);
    }
  }


  //</editor-fold>

  /**
   * Gets the singleton run entry for this database.
   * @return The entry.
   */
  public RunEntry getEntry() {
    return runEnt;
  }

  /**
   * @return A list of all workloads created by this entry
   * @throws SQLException
   */
  public List<WorkloadEntry> findWorkloads() throws SQLException {
    return daoWorkload.queryBuilder().query();
  }

  /**
   * Find handles which were created by this workload
   * @param workload The workload for which the child handles should be returned
   * @return A collection of child handle entries.
   * @throws SQLException
   */
  public List<HandleEntry> findHandles(WorkloadEntry workload) throws SQLException {
    return daoHandle.queryForEq(HandleEntry.FLD_WORKLOAD, workload);
  }


  /**
   * Retrieves all logs for this database
   * @return A list of logs
   * @throws SQLException
   */
  public List<LogEntry> getAllLogs() throws SQLException {
    return daoLog.queryBuilder()
            .orderBy(LogEntry.FLD_TIMESTAMP, true)
            .query();
  }



}