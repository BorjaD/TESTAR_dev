/***************************************************************************************************
 *
 * Copyright (c) 2020 Universitat Politecnica de Valencia - www.upv.es
 * Copyright (c) 2020 Open Universiteit - www.ou.nl
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************************************/


package nl.ou.testar.StateModel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.swing.JComboBox;

import org.fruit.monkey.Main;

import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.core.db.OrientDBConfig;
import com.orientechnologies.orient.core.record.ODirection;
import com.orientechnologies.orient.core.record.OEdge;
import com.orientechnologies.orient.core.record.OVertex;
import com.orientechnologies.orient.core.record.impl.ORecordBytes;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultSet;

import nl.ou.testar.StateModel.Persistence.OrientDB.Entity.Config;
import nl.ou.testar.a11y.reporting.HTMLReporter;

public class ModelDifferenceManager {

	private ModelDifferenceManager() {}

	// orient db instance that will create database sessions
	private static OrientDB orientDB;

	// orient db configuration object
	private static Config dbConfig;

	public static void obtainAvailableDatabases(String storeType, String storeServer, String root, String passField,
			JComboBox<String> listDatabases) {
		dbConfig = new Config();
		dbConfig.setConnectionType(storeType);
		dbConfig.setServer(storeServer);
		dbConfig.setUser(root);
		dbConfig.setPassword(passField);

		try{

			listDatabases.removeAllItems();

			orientDB = new OrientDB(dbConfig.getConnectionType() + ":" + dbConfig.getServer(), 
					dbConfig.getUser(), dbConfig.getPassword(), OrientDBConfig.defaultConfig());

			if(!orientDB.list().isEmpty())
				for(String database : orientDB.list())
					listDatabases.addItem(database);

		}catch(Exception e) {
			System.out.println(e.getMessage());
		}finally {
			orientDB.close();
		}

	}

	public static String connectionStuff(String storeType, String storeServer, String root, String passField,
			String database) {
		dbConfig = new Config();
		dbConfig.setConnectionType(storeType);
		dbConfig.setServer(storeServer);
		dbConfig.setUser(root);
		dbConfig.setPassword(passField);
		dbConfig.setDatabase(database);

		orientDB = new OrientDB(dbConfig.getConnectionType() + ":" + dbConfig.getServer(), 
				dbConfig.getUser(), dbConfig.getPassword(), OrientDBConfig.defaultConfig());

		return dbConfig.getConnectionType() + ":" + dbConfig.getServer() + "/database/" + dbConfig.getDatabase();
	}

	public static void closeOrientDB() {
		if(orientDB!=null && orientDB.isOpen())
			orientDB.close();
	}

	public static void calculateModelDifference(String storeType, String storeServer, String root, String passField,
			String database, String appNameOne, String appVerOne, String appNameTwo, String appVerTwo) {
		if(appNameOne == null || appVerOne == null || appNameTwo == null || appVerTwo == null)
			return;

		String dbConnection = connectionStuff(storeType, storeServer, root, passField, database);

		try (ODatabaseSession sessionDB = orientDB.open(dbConnection, dbConfig.getUser(), dbConfig.getPassword())){

			String modelIdOne = abstractStateModelInfo(sessionDB, appNameOne, appVerOne);
			Set<String> abstractStateOne = new HashSet<>(abstractState(sessionDB, modelIdOne));
			Set<String> abstractActionOne = new HashSet<>(abstractAction(sessionDB, modelIdOne));

			String modelIdTwo = abstractStateModelInfo(sessionDB, appNameTwo, appVerTwo);
			Set<String> abstractStateTwo = new HashSet<>(abstractState(sessionDB, modelIdTwo));
			Set<String> abstractActionTwo = new HashSet<>(abstractAction(sessionDB, modelIdTwo));

			//TODO: instead of (for) prepare a better Set difference comparison or,
			//TODO: prepare OrientDB queries to obtain the difference at DB level

			Set<String> dissapearedStatesImages = new HashSet<>();
			Set<String> dissapearedActionsDesc = new HashSet<>();
			
			Set<String> newStatesImages = new HashSet<>();
			Set<String> newActionsDesc = new HashSet<>();

			System.out.println("\n ---- DISSAPEARED ABSTRACT STATES ----");
			for(String s : abstractStateOne)
				if(!abstractStateTwo.contains(s)) {
					System.out.println(s);
					dissapearedStatesImages.add(screenshotConcreteState(sessionDB, concreteStateId(sessionDB, s), "DissapearedState"));
				}

			System.out.println("\n ---- NEW ABSTRACT STATES ----");
			for(String s : abstractStateTwo)
				if(!abstractStateOne.contains(s)) {
					System.out.println(s);
					newStatesImages.add(screenshotConcreteState(sessionDB, concreteStateId(sessionDB, s), "NewState"));
				}

			System.out.println("\n ---- DISSAPEARED ABSTRACT ACTIONS ----");
			for(String s : abstractActionOne)
				if(!abstractActionTwo.contains(s)) {
					System.out.println(s);
					dissapearedActionsDesc.add(concreteActionDesc(sessionDB, s));
				}

			System.out.println("\n ---- NEW ABSTRACT ACTIONS ----");
			for(String s : abstractActionTwo)
				if(!abstractActionOne.contains(s)) {
					System.out.println(s);
					newActionsDesc.add(concreteActionDesc(sessionDB, s));
				}
			
			createHTMLreport(dissapearedStatesImages, newStatesImages, dissapearedActionsDesc, newActionsDesc);

		}catch(Exception e) {
			e.printStackTrace();
		} finally {
			orientDB.close();
		}

	}

	/**
	 * Obtain the existing State Model Identifier from the name and version of one application
	 * 
	 * @param sessionDB
	 * @param appName
	 * @param appVer
	 * @return modelIdentifier
	 */
	private static String abstractStateModelInfo(ODatabaseSession sessionDB, String appName, String appVer) {
		
		String stmt = "SELECT FROM AbstractStateModel where applicationName = :applicationName and "
				+ "applicationVersion = :applicationVersion";
		
		Map<String, Object> params = new HashMap<>();
		params.put("applicationName", appName);
		params.put("applicationVersion", appVer);
		
		OResultSet resultSet = sessionDB.query(stmt, params);

		while (resultSet.hasNext()) {
			OResult result = resultSet.next();
			// we're expecting a vertex
			if (result.isVertex()) {
				Optional<OVertex> op = result.getVertex();
				if (!op.isPresent()) continue;
				OVertex modelVertex = op.get();

				System.out.println("StateModel: " + appName + " " + appVer);
				System.out.println("Collecting DB State Model data...");

				System.out.println("JSON: " + result.toJSON());
				System.out.println("Edges: " + modelVertex.getEdges(ODirection.BOTH));

				return modelVertex.getProperty("modelIdentifier");
			}
		}
		resultSet.close();

		return "";
	}

	/**
	 * Obtain all existing abstractStates identifiers of one State Model using his Identifier
	 * 
	 * @param sessionDB
	 * @param modelIdentifier
	 * @return All existing Abstract States identifiers
	 */
	private static Set<String> abstractState(ODatabaseSession sessionDB, String modelIdentifier) {
		
		Set<String> abstractStates = new HashSet<>();
		
		String stmt = "SELECT FROM AbstractState where modelIdentifier = :modelIdentifier";
		
		Map<String, Object> params = new HashMap<>();
		params.put("modelIdentifier", modelIdentifier);
		
		OResultSet resultSet = sessionDB.query(stmt, params);
		
		System.out.println("**** Existing AbstractStates ****");

		while (resultSet.hasNext()) {
			OResult result = resultSet.next();
			// we're expecting a vertex
			if (result.isVertex()) {
				Optional<OVertex> op = result.getVertex();
				if (!op.isPresent()) continue;
				OVertex modelVertex = op.get();

				System.out.println("JSON: " + result.toJSON());
				System.out.println("Edges: " + modelVertex.getEdges(ODirection.BOTH));
				abstractStates.add(modelVertex.getProperty("stateId"));
			}
		}
		resultSet.close();

		return abstractStates;
	}

	/**
	 * Obtain all existing abstractActions identifiers of one State Model using his Identifier
	 * 
	 * @param sessionDB
	 * @param modelIdentifier
	 * @return All existing Abstract Actions identifiers
	 */
	private static Set<String> abstractAction(ODatabaseSession sessionDB, String modelIdentifier) {
		
		Set<String> abstractActions = new HashSet<>();
		
		String stmt = "SELECT FROM AbstractAction where modelIdentifier = :modelIdentifier";
		
		Map<String, Object> params = new HashMap<>();
		params.put("modelIdentifier", modelIdentifier);
		
		OResultSet resultSet = sessionDB.query(stmt, params);

		System.out.println("**** Existing AbstractActions ****");

		while (resultSet.hasNext()) {
			OResult result = resultSet.next();
			// we're expecting a vertex
			if (result.isEdge()) {
				Optional<OEdge> op = result.getEdge();
				if (!op.isPresent()) continue;
				OEdge modelEdge = op.get();

				System.out.println("JSON: " + result.toJSON());
				abstractActions.add(modelEdge.getProperty("actionId"));
			}
		}
		resultSet.close();

		return abstractActions;
	}
	
	/**
	 * Obtain the ConcreteAction Description from an AbstractAction Identifier
	 * 
	 * @param sessionDB
	 * @param AbstractActionId
	 * @return ConcreteActionDescription
	 */
	private static String concreteActionDesc(ODatabaseSession sessionDB, String abstractActionId) {
		
		String concreteActionsDescription = "";
		
		String stmtAbstract = "SELECT FROM AbstractAction WHERE actionId = :actionId";
		
		Map<String, Object> paramsAbstract = new HashMap<>();
		paramsAbstract.put("actionId", abstractActionId);
		
		OResultSet resultSetAbstract = sessionDB.query(stmtAbstract, paramsAbstract);

		if (resultSetAbstract.hasNext()) {
			OResult resultAbstract = resultSetAbstract.next();
			// we're expecting an edge
			if (resultAbstract.isEdge()) {
				Optional<OEdge> opAbstract = resultAbstract.getEdge();
				if (!opAbstract.isPresent()) return "";
				OEdge modelEdgeAbstract = opAbstract.get();

				try {
					for(String concreteActionId : (Set<String>) modelEdgeAbstract.getProperty("concreteActionIds"))
						if(!concreteActionId.isEmpty()) {
							
							String stmtConcrete = "SELECT FROM ConcreteAction WHERE actionId = :actionId";
							
							Map<String, Object> paramsConcrete = new HashMap<>();
							paramsConcrete.put("actionId", concreteActionId);
							
							OResultSet resultSetConcrete = sessionDB.query(stmtConcrete, paramsConcrete);
							
							if (resultSetConcrete.hasNext()) {
								OResult resultConcrete = resultSetConcrete.next();
								// we're expecting a vertex
								if (resultConcrete.isEdge()) {
									Optional<OEdge> opConcrete = resultConcrete.getEdge();
									if (!opConcrete.isPresent()) continue;
									OEdge modelEdgeConcrete = opConcrete.get();
									
									concreteActionsDescription = modelEdgeConcrete.getProperty("Desc");
								}
							}
							
							resultSetConcrete.close();
							
							if(!concreteActionsDescription.isEmpty()) break;
							
						}
				}catch (Exception e) {System.out.println("ERROR: ModelDifferenceManager concreteActionIds() ");}
			}
		}
		
		resultSetAbstract.close();

		return concreteActionsDescription;
	}

	/**
	 * Obtain one Concrete State Identifier of one Abstract State Identifier
	 * 
	 * @param sessionDB
	 * @param stateId
	 * @return Concrete State Identifier
	 */
	private static String concreteStateId(ODatabaseSession sessionDB, String stateId) {
		
		String stmt = "SELECT FROM AbstractState WHERE stateId = :stateId LIMIT 1";
		
		Map<String, Object> params = new HashMap<>();
		params.put("stateId", stateId);
		
		OResultSet resultSet = sessionDB.query(stmt, params);

		while (resultSet.hasNext()) {
			OResult result = resultSet.next();
			// we're expecting a vertex
			if (result.isVertex()) {
				Optional<OVertex> op = result.getVertex();
				if (!op.isPresent()) continue;
				OVertex modelVertex = op.get();

				try {
					for(String concreteStateId : (Set<String>) modelVertex.getProperty("concreteStateIds"))
						if(!concreteStateId.isEmpty())
							return concreteStateId;
				}catch (Exception e) {System.out.println("ERROR: ModelDifferenceManager concreteStateId() ");}

			}
		}
		resultSet.close();

		return "";
	}

	/**
	 * Create and return the path of one Concrete State Screenshot by the identifier
	 * 
	 * @param sessionDB
	 * @param concreteId
	 * @param folderName
	 * @return path of existing screenshot
	 */
	private static String screenshotConcreteState(ODatabaseSession sessionDB, String concreteId, String folderName) {
		
		String stmt = "SELECT FROM ConcreteState WHERE ConcreteIDCustom = :concreteId LIMIT 1";
		
		Map<String, Object> params = new HashMap<>();
		params.put("concreteId", concreteId);
		
		OResultSet resultSet = sessionDB.query(stmt, params);

		while (resultSet.hasNext()) {
			OResult result = resultSet.next();
			// we're expecting a vertex
			if (result.isVertex()) {
				Optional<OVertex> op = result.getVertex();
				if (!op.isPresent()) continue;
				OVertex modelVertex = op.get();

				String sourceScreenshot = "n" + formatId(modelVertex.getIdentity().toString());
				return processScreenShot(modelVertex.getProperty("screenshot"), sourceScreenshot, folderName);
			}
		}
		resultSet.close();
		return "";
	}

	// this helper method formats the @RID property into something that can be used in a web frontend
	private static String formatId(String id) {
		if (id.indexOf("#") != 0) return id; // not an orientdb id
		id = id.replaceAll("[#]", "");
		return id.replaceAll("[:]", "_");
	}

	/**
	 * This method saves screenshots to disk.
	 * @param recordBytes
	 * @param identifier
	 */
	private static String processScreenShot(ORecordBytes recordBytes, String identifier, String folderName) {
		if (!Main.outputDir.substring(Main.outputDir.length() - 1).equals(File.separator)) {
			Main.outputDir += File.separator;
		}

		// see if we have a directory for the screenshots yet
		File screenshotDir = new File(Main.outputDir + "ModelDiff" + /*File.separator + folderName +*/ File.separator);

		if (!screenshotDir.exists()) {
			screenshotDir.mkdir();
		}

		// save the file to disk
		File screenshotFile = new File( screenshotDir, identifier + ".png");
		if (screenshotFile.exists()) {
			try {
				return screenshotFile.getCanonicalPath();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			FileOutputStream outputStream = new FileOutputStream(screenshotFile.getCanonicalPath());
			outputStream.write(recordBytes.toStream());
			outputStream.flush();
			outputStream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		try {
			return screenshotFile.getCanonicalPath();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return "";

	}

	private static void createHTMLreport(Set<String> dissapearedStatesImages, Set<String> newStatesImages, 
			Set<String> dissapearedActionsDesc, Set<String> newActionsDesc) {
		try {
			String[] HEADER = new String[] {
					"<!DOCTYPE html>",
					"<html>",
					"<head>",
					"<title>TESTAR State Model difference report</title>",
					"</head>",
					"<body>"
			};

			String htmlReportName = Main.outputDir + "ModelDiff" + File.separator + "DifferenceReport.html";

			PrintWriter out = new PrintWriter(new File(htmlReportName).getCanonicalPath(), HTMLReporter.CHARSET);

			for(String s:HEADER){
				out.println(s);
				out.flush();
			}

			out.println("<h4> Dissapeared Abstract States </h4>");
			out.flush();

			for(String path : dissapearedStatesImages) {
				out.println("<p><img src=\""+path+"\"></p>");
				out.flush();
			}
			
			out.println("<h2> Dissapeared Actions, Concrete Description </h2>");
			out.flush();

			for(String desc : dissapearedActionsDesc) {
				out.println("<p>" + desc + "</p>");
				out.flush();
			}
			
			out.println("<h4> New Abstract States </h4>");
			out.flush();

			for(String path : newStatesImages) {
				out.println("<p><img src=\""+path+"\"></p>");
				out.flush();
			}
			
			out.println("<h2> New Actions Discovered, Concrete Description </h2>");
			out.flush();

			for(String desc : newActionsDesc) {
				out.println("<p>" + desc + "</p>");
				out.flush();
			}
			
			out.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
