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

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.fruit.Pair;
import org.fruit.monkey.Main;
import org.testar.json.object.JsonArtefactModelDifference;

import com.google.common.collect.Sets;
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

	// Identifiers of the State Models we want to compare
	private static String identifierModelOne;
	private static String identifierModelTwo;

	// Set Collection with all existing Abstract States of the State Models we want to compare
	private static Set<String> allAbstractStatesModelOne = new HashSet<>();
	private static Set<String> allAbstractStatesModelTwo = new HashSet<>();

	// Prepare a Collection to save all disappeared Abstract States
	private static Set<String> disappearedAbstractStates = new HashSet<>();
	// Prepare a Map to associate an Abstract State identifier with a visual Screenshot (we will find a Concrete State)  
	private static HashMap<String, String> disappearedStatesImages = new HashMap<>();
	// Prepare a Map to associate an Abstract State identifier with all the disappeared Action Description
	private static HashMap<String, Set<Pair<String, String>>> disappearedActions = new HashMap<>();

	// Prepare a Collection to save all new Abstract States
	private static Set<String> newAbstractStates = new HashSet<>();
	// Prepare a Map to associate an Abstract State identifier with a visual Screenshot (we will find a Concrete State)
	private static HashMap<String, String> newStatesImages = new HashMap<>();
	// Prepare a Map to associate an Abstract State identifier with all the new Action Description
	private static HashMap<String, Set<Pair<String, String>>> newActions = new HashMap<>();

	private ModelDifferenceManager() {}

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

			identifierModelOne = abstractStateModelIdentifier(sessionDB, appNameOne, appVerOne);
			identifierModelTwo = abstractStateModelIdentifier(sessionDB, appNameTwo, appVerTwo);

			allAbstractStatesModelOne = new HashSet<>(abstractState(sessionDB, identifierModelOne));
			allAbstractStatesModelTwo = new HashSet<>(abstractState(sessionDB, identifierModelTwo));

			disappearedAbstractStates = new HashSet<>();
			disappearedStatesImages = new HashMap<>();
			disappearedActions = new HashMap<>();

			newAbstractStates = new HashSet<>();
			newStatesImages = new HashMap<>();
			newActions = new HashMap<>();

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

					disappearedActions.put(abstractStateId, outgoingActionIdDesc(sessionDB, identifierModelOne, abstractStateId));
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

					newActions.put(abstractStateId, outgoingActionIdDesc(sessionDB, identifierModelTwo, abstractStateId));
				}
			});

			createHTMLreport(sessionDB);

			JsonArtefactModelDifference.createModelDifferenceArtefact(
					Arrays.asList(appNameOne, appVerOne, identifierModelOne),
					Arrays.asList(appNameTwo, appVerTwo, identifierModelTwo),
					disappearedAbstractStates, newAbstractStates,
					disappearedActions, newActions);

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
	private static String abstractStateModelIdentifier(ODatabaseSession sessionDB, String appName, String appVer) {

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
	 * The State we are going from an Action
	 * 
	 * @param sessionDB
	 * @param abstractActionId
	 * @return
	 */
	private static String abstractStateFromAction(ODatabaseSession sessionDB, String abstractActionId) {

		String abstractState = "";

		String stmt = "SELECT FROM AbstractAction where actionId = :abstractActionId";

		Map<String, Object> params = new HashMap<>();
		params.put("abstractActionId", abstractActionId);

		OResultSet resultSet = sessionDB.query(stmt, params);

		if (resultSet.hasNext()) {
			OResult result = resultSet.next();
			// we're expecting a vertex
			if (result.isEdge()) {
				Optional<OEdge> op = result.getEdge();
				if (!op.isPresent()) return "";
				OEdge modelEdge = op.get();

				return modelEdge.getVertex(ODirection.IN).getProperty("stateId");
			}
		}
		resultSet.close();

		return abstractState;
	}

	/**
	 * Return a List of Pairs with Concrete Actions <Id, Description> from one Specific AbstractState
	 * 
	 * @param sessionDB
	 * @param modelIdentifier
	 * @param abstractStateId
	 * @return
	 */
	private static Set<Pair<String, String>> outgoingActionIdDesc(ODatabaseSession sessionDB, String modelIdentifier, String abstractStateId) {

		Set<Pair<String, String>> outgoingActionIdDesc = new HashSet<>();

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
				if (!op.isPresent()) return outgoingActionIdDesc;
				OVertex modelVertex = op.get();

				// Abstract Actions Edges
				for(OEdge modelEdge : modelVertex.getEdges(ODirection.OUT)) {
					String abstractActionId = modelEdge.getProperty("actionId");
					Pair<String,String> pair = new Pair(abstractActionId, concreteActionDesc(sessionDB, abstractActionId));
					outgoingActionIdDesc.add(pair);
				}
			}
		}
		resultSet.close();

		return outgoingActionIdDesc;
	}

	/**
	 * Return a Map of all Incoming AbstractActions to one Specific AbstractState
	 * 
	 * @param sessionDB
	 * @param modelIdentifier
	 * @return
	 */
	private static Set<Pair<String,String>> incomingActionsIdDesc(ODatabaseSession sessionDB, String modelIdentifier, String abstractStateId) {

		Set<Pair<String,String>> incomingActionsIdDesc = new HashSet<>();

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
				if (!op.isPresent()) return incomingActionsIdDesc;
				OVertex modelVertex = op.get();

				// Abstract Actions Edges
				for(OEdge modelEdge : modelVertex.getEdges(ODirection.IN)) {
					String abstractActionId = modelEdge.getProperty("actionId");
					Pair<String,String> pair = new Pair(abstractActionId, concreteActionDesc(sessionDB, abstractActionId));
					incomingActionsIdDesc.add(pair);
				}
			}
		}
		resultSet.close();

		return incomingActionsIdDesc;
	}

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

	private static void createHTMLreport(ODatabaseSession sessionDB) {

		try {
			String[] HEADER = new String[] {
					"<!DOCTYPE html>",
					"<html>",
					"<style>",
					".container {display: flex;}",
					".float {display:inline-block;}",
					"</style>",
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

			out.println("<h2> Disappeared Abstract States: " + disappearedAbstractStates.size() + "</h2>");
			out.println("<div class=\"container\">");
			out.flush();


			for(String dissState :  disappearedAbstractStates) {

				out.println("<div class=<\"float\">");
				
				out.println("<p><img src=\"" + disappearedStatesImages.get(dissState) + "\"></p>");
				
				out.println("<h4> Disappeared Actions of this State, Concrete Description </h4>");
				out.flush();

				for(Pair<String,String> action : disappearedActions.get(dissState)) {

					// This if will not happen because Actions are currently State dependent (new State means all Actions are new)
					if(incomingActionsIdDesc(sessionDB, identifierModelTwo, abstractStateFromAction(sessionDB, action.left())).contains(action)) {
						out.println("<p style=\"color:green;\">" + action.right() + "</p>");
						out.flush();
					} else {
						out.println("<p style=\"color:red;\">" + action.right() + "</p>");
						out.flush();
					}
				}
				
				out.println("</div>");
				out.flush();
			}
			
			out.println("</div>");
					
			out.println("<h2> New Abstract States: " + newAbstractStates.size() + "</h2>");
			out.println("<div class=\"container\">");
			out.flush();


			for(String newState : newAbstractStates) {
				
				out.println("<div class=<\"float\">");

				out.println("<p><img src=\"" + newStatesImages.get(newState) + "\"></p>");

				out.println("<h4> New Actions Discovered on this State, Concrete Description </h4>");
				out.flush();


				for(Pair<String,String> action : newActions.get(newState)) {

					// This if will not happen because Actions are currently State dependent (new State means all Actions are new)
					if(incomingActionsIdDesc(sessionDB, identifierModelOne, abstractStateFromAction(sessionDB, action.left())).contains(action)) {
						out.println("<p style=\"color:green;\">" + action.right() + "</p>");
						out.flush();
					} else {
						out.println("<p style=\"color:red;\">" + action.right() + "</p>");
						out.flush();
					}
				}
				
				out.println("</div>");
				out.flush();
			}
			
			out.println("</div>");

			// Image or Widget Tree comparison
			out.println("<h2> Specific State changes </h2>");
			out.flush();

			// new States of Model Two to be compared with Model One
			for(String newStateModelTwo : newAbstractStates) {

				Set<Pair<String, String>> incomingActionsModelTwo = incomingActionsIdDesc(sessionDB, identifierModelTwo, newStateModelTwo);
				incomingActionsModelTwo.remove(new Pair<String, String>(null,""));
				
				for(String dissStateModelOne :  disappearedAbstractStates) {

					Set<Pair<String, String>> incomingActionsModelOne = incomingActionsIdDesc(sessionDB, identifierModelOne, dissStateModelOne);
					incomingActionsModelOne.remove(new Pair<String, String>(null,""));
					
					if(!Sets.intersection(incomingActionsModelTwo, incomingActionsModelOne).isEmpty()) {

						String diffDisk = getDifferenceImage(disappearedStatesImages.get(dissStateModelOne), dissStateModelOne,
								newStatesImages.get(newStateModelTwo), newStateModelTwo);


						out.println("<p><img src=\"" + disappearedStatesImages.get(dissStateModelOne) + "\">");
						out.println("<img src=\"" + newStatesImages.get(newStateModelTwo) + "\">");
						out.println("<img src=\"" + diffDisk + "\"></p>");
						
						out.println("<p style=\"color:blue;\">" + "We have reached this State with Action: " + Sets.intersection(incomingActionsModelTwo, incomingActionsModelOne) + "</p>");
						
						out.flush();
					}
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

	// https://stackoverflow.com/questions/25022578/highlight-differences-between-images
	private static String getDifferenceImage(String img1Disk, String idImg1, String img2Disk, String idImg2) {
		try {

			BufferedImage img1 = ImageIO.read(new File(img1Disk));
			BufferedImage img2 = ImageIO.read(new File(img2Disk));

			int width1 = img1.getWidth(); // Change - getWidth() and getHeight() for BufferedImage
			int width2 = img2.getWidth(); // take no arguments
			int height1 = img1.getHeight();
			int height2 = img2.getHeight();
			if ((width1 != width2) || (height1 != height2)) {
				System.err.println("Error: Images dimensions mismatch");
				System.exit(1);
			}

			// NEW - Create output Buffered image of type RGB
			BufferedImage outImg = new BufferedImage(width1, height1, BufferedImage.TYPE_INT_RGB);

			// Modified - Changed to int as pixels are ints
			int diff;
			int result; // Stores output pixel
			for (int i = 0; i < height1; i++) {
				for (int j = 0; j < width1; j++) {
					int rgb1 = img1.getRGB(j, i);
					int rgb2 = img2.getRGB(j, i);
					int r1 = (rgb1 >> 16) & 0xff;
					int g1 = (rgb1 >> 8) & 0xff;
					int b1 = (rgb1) & 0xff;
					int r2 = (rgb2 >> 16) & 0xff;
					int g2 = (rgb2 >> 8) & 0xff;
					int b2 = (rgb2) & 0xff;
					diff = Math.abs(r1 - r2); // Change
					diff += Math.abs(g1 - g2);
					diff += Math.abs(b1 - b2);
					diff /= 3; // Change - Ensure result is between 0 - 255
					// Make the difference image gray scale
					// The RGB components are all the same
					result = (diff << 16) | (diff << 8) | diff;
					outImg.setRGB(j, i, result); // Set result
				}
			}

			// Now save the image on disk

			if (!Main.outputDir.substring(Main.outputDir.length() - 1).equals(File.separator)) {
				Main.outputDir += File.separator;
			}

			// see if we have a directory for the screenshots yet
			File screenshotDir = new File(Main.outputDir + "ModelDiff" + /*File.separator + folderName +*/ File.separator);

			if (!screenshotDir.exists()) {
				screenshotDir.mkdir();
			}

			// save the file to disk
			File screenshotFile = new File( screenshotDir, "diff_"+ idImg1 + "_" + idImg2 + ".png");
			if (screenshotFile.exists()) {
				try {
					return screenshotFile.getCanonicalPath();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			FileOutputStream outputStream = new FileOutputStream(screenshotFile.getCanonicalPath());

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(outImg, "png", baos);
			byte[] bytes = baos.toByteArray();

			outputStream.write(bytes);
			outputStream.flush();
			outputStream.close();

			return screenshotFile.getCanonicalPath();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		return "";
	}


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
}
