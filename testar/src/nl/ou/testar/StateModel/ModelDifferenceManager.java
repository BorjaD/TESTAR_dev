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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.fruit.monkey.Main;
import org.testar.json.object.JsonArtefactModelDifference;

import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.core.db.OrientDBConfig;
import com.orientechnologies.orient.core.exception.OSecurityAccessException;
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

	public static void obtainAvailableDatabases(String storeType, String storeServer, String storeDirectory,
			String root, String passField, JComboBox<String> listDatabases) {
		dbConfig = new Config();
		dbConfig.setConnectionType(storeType);
		dbConfig.setServer(storeServer);
		dbConfig.setUser(root);
		dbConfig.setPassword(passField);
		dbConfig.setDatabaseDirectory(storeDirectory);

		try{

			listDatabases.removeAllItems();

	        String connectionString = dbConfig.getConnectionType() + ":" + (dbConfig.getConnectionType().equals("remote") ?
	                dbConfig.getServer() : dbConfig.getDatabaseDirectory()) + "/";
			
			orientDB = new OrientDB(connectionString, dbConfig.getUser(), dbConfig.getPassword(), OrientDBConfig.defaultConfig());

			if(!orientDB.list().isEmpty())
				for(String database : orientDB.list())
					listDatabases.addItem(database);

		} catch(OSecurityAccessException e) {
			JFrame frame = new JFrame();
			JOptionPane.showMessageDialog(frame, 
					" User or password not valid for database: " + listDatabases.getSelectedItem().toString() + 
					"\n plocal databases do not use 'root' user" + 
					"\n try with customized user");
			frame.setAlwaysOnTop(true);
		} catch(Exception e) {
			System.out.println(e.getMessage());
		} finally {
			orientDB.close();
		}

	}

	public static void connectionStuff(String storeType, String storeServer, String storeDirectory,
			String root, String passField, String database) {
		dbConfig = new Config();
		dbConfig.setConnectionType(storeType);
		dbConfig.setServer(storeServer);
		dbConfig.setUser(root);
		dbConfig.setPassword(passField);
		dbConfig.setDatabase(database);
		dbConfig.setDatabaseDirectory(storeDirectory);

        String connectionString = dbConfig.getConnectionType() + ":" + (dbConfig.getConnectionType().equals("remote") ?
                dbConfig.getServer() : dbConfig.getDatabaseDirectory()) + "/";
        
        orientDB = new OrientDB(connectionString, dbConfig.getUser(), dbConfig.getPassword(), OrientDBConfig.defaultConfig());
	}

	public static void closeOrientDB() {
		if(orientDB!=null && orientDB.isOpen())
			orientDB.close();
	}

	public static void calculateModelDifference(String storeType, String storeServer, String storeDirectory, String root, String passField,
			String database, String appNameOne, String appVerOne, String appNameTwo, String appVerTwo) {
		if(appNameOne == null || appVerOne == null || appNameTwo == null || appVerTwo == null)
			return;

		connectionStuff(storeType, storeServer, storeDirectory, root, passField, database);

		try (ODatabaseSession sessionDB = orientDB.open(dbConfig.getDatabase(), dbConfig.getUser(), dbConfig.getPassword())){

			/**
			 * TODO: instead of (for) prepare a better Set difference comparison or
			 * TODO: prepare OrientDB queries to obtain the difference at DB level
			 */
			
			// Identifiers of the State Models we want to compare
			String identifierModelOne = abstractStateModelInfo(sessionDB, appNameOne, appVerOne);
			String identifierModelTwo = abstractStateModelInfo(sessionDB, appNameTwo, appVerTwo);
			
			// Set Collection with all existing Abstract States of the State Models we want to compare
			Set<String> allAbstractStatesModelOne = new HashSet<>(abstractState(sessionDB, identifierModelOne));
			Set<String> allAbstractStatesModelTwo = new HashSet<>(abstractState(sessionDB, identifierModelTwo));
			
			// Map < Abstract State Id, Set < All Abstract Actions > >
			// Associate all existing Abstract States Identifiers with the Collection of available Abstract Actions on each Abstract State
			//HashMap<String, Set<String>> stateIdWithAllActionsModelOne = new HashMap<>(abstractAction(sessionDB, identifierModelOne));
			//HashMap<String, Set<String>> stateIdWithAllActionsModelTwo = new HashMap<>(abstractAction(sessionDB, identifierModelTwo));

			// Prepare a Collection to save all disappeared Abstract States
			Set<String> disappearedAbstractStates = new HashSet<>();
			// Prepare a Map to associate an Abstract State identifier with a visual Screenshot (we will find a Concrete State)  
			HashMap<String, String> disappearedStatesImages = new HashMap<>();
			// Prepare a Map to associate an Abstract State identifier with all the disappeared Action Description
			HashMap<String, Set<String>> disappearedActionsDesc = new HashMap<>();
			
			// Prepare a Collection to save all new Abstract States
			Set<String> newAbstractStates = new HashSet<>();
			// Prepare a Map to associate an Abstract State identifier with a visual Screenshot (we will find a Concrete State)
			HashMap<String, String> newStatesImages = new HashMap<>();
			// Prepare a Map to associate an Abstract State identifier with all the new Action Description
			HashMap<String, Set<String>> newActionsDesc = new HashMap<>();

			/**
			 * Check which Abstract States of Model One don't exists at Model Two
			 * Disappeared Abstract States
			 */
			
			allAbstractStatesModelOne.forEach( abstractStateId -> {

				// Only if doesn't exists in the State Model Two
				if(!allAbstractStatesModelTwo.contains(abstractStateId)) {

					disappearedAbstractStates.add(abstractStateId);

					String screenshotPath = screenshotConcreteState(sessionDB, concreteStateId(sessionDB, abstractStateId), "disappearedState");
					disappearedStatesImages.put(abstractStateId, screenshotPath);

					disappearedActionsDesc.put(abstractStateId, actionDescFromAbstractState(sessionDB, identifierModelOne, abstractStateId));
					
					// Update a Description Action Collection with the disappeared, to update the Map disappearedActionsDesc
					/*stateIdWithAllActionsModelOne.values().iterator().next().stream()
					.filter( abstractActionId -> { return  abstractActionId != null; })
					.forEach( abstractActionId -> {

						if(disappearedActionsDesc.get(abstractStateId) == null) {
							Set<String> descriptions = new HashSet<>();
							descriptions.add(concreteActionDesc(sessionDB, abstractActionId));
							disappearedActionsDesc.put(abstractStateId, descriptions);
						} else {
							Set<String> descriptions = disappearedActionsDesc.get(abstractStateId);
							descriptions.add(concreteActionDesc(sessionDB, abstractActionId));
							disappearedActionsDesc.put(abstractStateId, descriptions);
						}
					});*/

				}

			});

			/**
			 * Check which Abstract States of Model Two don't exists at Model One
			 * New Abstract States
			 */
			
			allAbstractStatesModelTwo.forEach( abstractStateId -> {
				
				// Only if doesn't exists in the State Model One
				if(!allAbstractStatesModelOne.contains(abstractStateId)) {
					
					newAbstractStates.add(abstractStateId);
					
					String screenshotPath = screenshotConcreteState(sessionDB, concreteStateId(sessionDB, abstractStateId), "NewState");
					newStatesImages.put(abstractStateId, screenshotPath);
					
					newActionsDesc.put(abstractStateId, actionDescFromAbstractState(sessionDB, identifierModelTwo, abstractStateId));
					
					// Update a Description Action Collection with the news, to update the Map newActionsDesc
					/*stateIdWithAllActionsModelTwo.values().iterator().next().stream()
					.filter( abstractActionId -> { return  abstractActionId != null; })
					.forEach( abstractActionId -> {
						
						if(newActionsDesc.get(abstractStateId) == null) {
							Set<String> descriptions = new HashSet<>();
							descriptions.add(concreteActionDesc(sessionDB, abstractActionId));
							newActionsDesc.put(abstractStateId, descriptions);
						} else {
							Set<String> descriptions = newActionsDesc.get(abstractStateId);
							descriptions.add(concreteActionDesc(sessionDB, abstractActionId));
							newActionsDesc.put(abstractStateId, descriptions);
						}
						
					});*/
					
				}
				
			});
			
			createHTMLreport(
					disappearedAbstractStates, newAbstractStates,
					disappearedStatesImages, newStatesImages,
					disappearedActionsDesc, newActionsDesc);
			
			JsonArtefactModelDifference.createModelDifferenceArtefact(
					Arrays.asList(appNameOne, appVerOne, identifierModelOne),
					Arrays.asList(appNameTwo, appVerTwo, identifierModelTwo),
					disappearedAbstractStates, newAbstractStates,
					disappearedActionsDesc, newActionsDesc);

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

				return modelVertex.getProperty("modelIdentifier");
			}
		}
		resultSet.close();

		return "";
	}

	/**
	 * Obtain a Collection of all existing abstractStates identifiers of one State Model using his Identifier
	 * 
	 * @param sessionDB
	 * @param modelIdentifier
	 * @return Collection of Abstract States 
	 */
	private static Set<String> abstractState(ODatabaseSession sessionDB, String modelIdentifier) {
		
		Set<String> abstractStates = new HashSet<>();
		
		String stmt = "SELECT FROM AbstractState where modelIdentifier = :modelIdentifier";
		
		Map<String, Object> params = new HashMap<>();
		params.put("modelIdentifier", modelIdentifier);
		
		OResultSet resultSet = sessionDB.query(stmt, params);

		while (resultSet.hasNext()) {
			OResult result = resultSet.next();
			// we're expecting a vertex
			if (result.isVertex()) {
				Optional<OVertex> op = result.getVertex();
				if (!op.isPresent()) continue;
				OVertex modelVertex = op.get();
				
				abstractStates.add(modelVertex.getProperty("stateId"));
			}
		}
		resultSet.close();

		return abstractStates;
	}
	
	/**
	 * Return a List of Concrete Actions Description from one AbstractState
	 * 
	 * @param sessionDB
	 * @param modelIdentifier
	 * @param abstractStateId
	 * @return
	 */
	private static Set<String> actionDescFromAbstractState(ODatabaseSession sessionDB, String modelIdentifier, String abstractStateId) {
		Set<String> abstractActions = new HashSet<>();

		String stmt = "SELECT FROM AbstractState where modelIdentifier = :modelIdentifier and stateId = :abstractStateId";

		Map<String, Object> params = new HashMap<>();
		params.put("modelIdentifier", modelIdentifier);
		params.put("abstractStateId", abstractStateId);

		OResultSet resultSet = sessionDB.query(stmt, params);

		if (resultSet.hasNext()) {
			OResult result = resultSet.next();
			// Abstract State Vertex
			if (result.isVertex()) {
				Optional<OVertex> op = result.getVertex();
				if (!op.isPresent()) return abstractActions;
				OVertex modelVertex = op.get();

				// Abstract Actions Edges
				for(OEdge modelEdge : modelVertex.getEdges(ODirection.OUT)) {
					String abstractActionId = modelEdge.getProperty("actionId");
					abstractActions.add(concreteActionDesc(sessionDB, abstractActionId));
				}
			}
		}
		resultSet.close();

		return abstractActions;
	}

	/**
	 * Obtain a Map of all existing Abstract Actions of every Abstract State of one State Model using his Identifier
	 * 
	 * @param sessionDB
	 * @param modelIdentifier
	 * @return Map of all existing Abstract Actions of every Abstract State
	 */
	/* private static HashMap<String, Set<String>> abstractAction(ODatabaseSession sessionDB, String modelIdentifier) {
		
		HashMap<String, Set<String>> abstractActions = new HashMap<>();
		
		String stmt = "SELECT FROM AbstractAction where modelIdentifier = :modelIdentifier";
		
		Map<String, Object> params = new HashMap<>();
		params.put("modelIdentifier", modelIdentifier);
		
		OResultSet resultSet = sessionDB.query(stmt, params);

		while (resultSet.hasNext()) {
			OResult result = resultSet.next();
			// we're expecting a vertex
			if (result.isEdge()) {
				Optional<OEdge> op = result.getEdge();
				if (!op.isPresent()) continue;
				OEdge modelEdge = op.get();
				
				OVertex originAbstractState = modelEdge.getVertex(ODirection.OUT);
				
				String stateId = originAbstractState.getProperty("stateId");
				String actionId = modelEdge.getProperty("actionId");
				
				if(abstractActions.get(stateId) == null) {
					Set<String> actions = new HashSet<>();
					actions.add(actionId);
					abstractActions.put(stateId, actions);
				} else {
					Set<String> actions = abstractActions.get(stateId);
					actions.add(actionId);
					abstractActions.put(stateId, actions);
				}
			}
		}
		resultSet.close();

		return abstractActions;
	}*/
	
	/**
	 * Obtain the ConcreteAction Description from an Abstract Action Identifier
	 * 
	 * @param sessionDB
	 * @param AbstractActionId
	 * @return Concrete Action Description
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

	private static void createHTMLreport( Set<String> disappearedAbstractStates, Set<String> newAbstractStates,
			HashMap<String, String> disappearedStatesImages, HashMap<String, String> newStatesImages, 
			HashMap<String, Set<String>> disappearedActionsDesc, HashMap<String, Set<String>> newActionsDesc) {
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
			
			out.println("<h2> Disappeared Abstract States </h2>");
			out.flush();
			
			
			for(String dissState :  disappearedAbstractStates) {

				out.println("<p><img src=\"" + disappearedStatesImages.get(dissState) + "\"></p>");
				out.flush();
				
				out.println("<h4> Disappeared Actions of this State, Concrete Description </h4>");
				out.flush();
				
				for(String actionDesc : disappearedActionsDesc.get(dissState)) {
					
					out.println("<p>" + actionDesc + "</p>");
					out.flush();
				}
			}
			
			out.println("<h2> New Abstract States </h2>");
			out.flush();
			
			
			for(String newState : newAbstractStates) {

				out.println("<p><img src=\"" + newStatesImages.get(newState) + "\"></p>");
				out.flush();
				
				out.println("<h4> New Actions Discovered on this State, Concrete Description </h4>");
				out.flush();
				
				
				for(String actionDesc : newActionsDesc.get(newState)) {
					
					out.println("<p>" + actionDesc + "</p>");
					out.flush();
				}
			}
			
			out.close();
			
			System.out.println("\n ****************************************************************************************************** \n");
			System.out.println("TESTAR State Model Difference report created in: " + htmlReportName);
			System.out.println("\n ****************************************************************************************************** \n");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
