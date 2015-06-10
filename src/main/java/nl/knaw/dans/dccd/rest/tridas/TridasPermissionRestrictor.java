package nl.knaw.dans.dccd.rest.tridas;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.knaw.dans.dccd.model.ProjectPermissionLevel;

import org.tridas.interfaces.ITridasGeneric;
import org.tridas.schema.TridasDerivedSeries;
import org.tridas.schema.TridasElement;
import org.tridas.schema.TridasGenericField;
import org.tridas.schema.TridasMeasurementSeries;
import org.tridas.schema.TridasObject;
import org.tridas.schema.TridasProject;
import org.tridas.schema.TridasRadius;
import org.tridas.schema.TridasSample;
import org.tridas.schema.TridasValues;

public class TridasPermissionRestrictor {
	public static final String INCOMPLETE_TRIDAS_NOTE_GENERICFIELD_NAME = "dccd.incompleteTridasNote";
	public static final String INCOMPLETE_TRIDAS_ENTITY_PLACEHOLDER_GENERICFIELD_NAME = "dccd.incompleteTridas.entityPlaceholder";	

	@SuppressWarnings({ "rawtypes", "serial" })
	public static final Map<Class, String> MAP_ENTITY_TO_DISPLAYSTRING = 
		    Collections.unmodifiableMap(new HashMap<Class, String>() {{ 
		        put(TridasProject.class, "project");
		        put(TridasObject.class, "object");
		        put(TridasElement.class, "element");
		        put(TridasSample.class, "sample");
		        put(TridasRadius.class, "radius");
		        put(TridasMeasurementSeries.class, "measurementSeries");
		        put(TridasDerivedSeries.class, "derivedSeries");
		        put(TridasValues.class, "values");
		    }});

	/**
	 * Remove entities (or attributes) we are not permitted to see. 
	 * Also add placeholders to indicate that things have been removed and possibly 
	 * what has been removed.
	 * 
	 * Note that it changes the given tridas
	 * 
	 * @param tridasProject
	 * @param level
	 */
	public void restrictToPermitted(TridasProject tridasProject, ProjectPermissionLevel level)
	{
		switch(level)
		{
			case MINIMAL:
				// restrict Project and Object to open access 
				// and remove elements and sub-objects
				restrictToOpenAccess(tridasProject);
				break;
			case PROJECT:
				removeDerivedSeries(tridasProject, level);
				// restrict objects to open access 
				for(TridasObject object : tridasProject.getObjects())
					restrictToOpenAccess(object);
				// add a note on Project (top) level that we have removed information
				tridasProject.getGenericFields().add(createIncompleteTridasNote(level));
				break;
			case OBJECT:	
			case ELEMENT:	
			case SAMPLE:	
			case RADIUS:	
				removeDerivedSeries(tridasProject, level);
				// fallthrough
			case SERIES:
				for(TridasObject object : tridasProject.getObjects())
					restrictToPermitted(object, level);
				// restrict derived
				for(TridasDerivedSeries derivedSeries : tridasProject.getDerivedSeries())
					restrictToPermitted(derivedSeries, level);
				// add a note on Project (top) level that we have removed information
				tridasProject.getGenericFields().add(createIncompleteTridasNote(level));
				break;
			case VALUES: // allow all
				return; // Do not remove anything
		}		
	}
	
	protected void removeDerivedSeries(TridasProject tridasProject, ProjectPermissionLevel level)
	{
		List<TridasDerivedSeries> series = tridasProject.getDerivedSeries();
		addPlaceholders(tridasProject, MAP_ENTITY_TO_DISPLAYSTRING.get(TridasDerivedSeries.class), series.size(), level);
		series.clear();		
	}

	/**
	 * Remove parts that are not Open Access
	 * 
	 * @param tridasProject
	 */
	protected void restrictToOpenAccess(TridasProject tridasProject)
	{
		// remove attributes that are not open
		tridasProject.setCreatedTimestamp(null);
		tridasProject.setLastModifiedTimestamp(null);
		// Is it valid TRiDaS?
		tridasProject.setComments(null);
		tridasProject.setDescription(null);
		tridasProject.setPeriod(null);
		tridasProject.setCommissioner(null);
		
		// and lists
		tridasProject.getReferences().clear();
		tridasProject.getResearches().clear();
		tridasProject.getFiles().clear();
		tridasProject.getGenericFields().clear();
		tridasProject.getDerivedSeries().clear();	

		// Add note
		tridasProject.getGenericFields().add(createIncompleteTridasNoteForOpenAccess());

		// restrict objects
		for(TridasObject object : tridasProject.getObjects())
			restrictToOpenAccess(object);
	}
	
	/**
	 * Remove parts that are not Open Access
	 *
	 * @param object
	 */
	protected void restrictToOpenAccess(TridasObject object)
	{
		// remove attributes that are not open
		// allow only 'title' and 'type'
		object.setCreatedTimestamp(null);
		object.setLastModifiedTimestamp(null);
		object.setIdentifier(null);
		// Is it valid TRiDaS?
		object.setComments(null);
		object.setCoverage(null);
		object.setCreator(null);
		object.setDescription(null);
		object.setLinkSeries(null);
		object.setLocation(null);
		object.setOwner(null);

		// and lists		
		object.getFiles().clear();
		object.getGenericFields().clear();		
		object.getElements().clear();
		
		// Add note
		object.getGenericFields().add(createIncompleteTridasNoteForOpenAccess());

		//object.getObjects().clear();
		// sub-objects
		for(TridasObject subObject : object.getObjects())
			restrictToOpenAccess(subObject); // recursion!
	}
	
	/**
	 * Note indication things have been removed 
	 * specifically for open access
	 * 
	 * @return
	 */
	protected TridasGenericField createIncompleteTridasNoteForOpenAccess()
	{
		TridasGenericField genericField = new TridasGenericField();
		genericField.setName(INCOMPLETE_TRIDAS_NOTE_GENERICFIELD_NAME);
		genericField.setValue("This TRiDaS is incomplete. "
				+ "Only open access information is included!");
		return genericField;
	}
	
	/**
	 * Note indication things have been removed 
	 * 
	 * @param level
	 * @return
	 */
	protected TridasGenericField createIncompleteTridasNote(ProjectPermissionLevel level)
	{
		TridasGenericField genericField = new TridasGenericField();
		genericField.setName(INCOMPLETE_TRIDAS_NOTE_GENERICFIELD_NAME);
		genericField.setValue("This TRiDaS is incomplete. "
				+ "You are currently authorised to view no more than the "
				+ level.toString() + 
				" level of this project. ");
		return genericField;
	}
	
	/**
	 * Indicate what has been removed
	 * 
	 * @param entityLabel
	 * @param level
	 * @return
	 */
	protected TridasGenericField createRemovedTridasEntityPlaceholder(String entityLabel, ProjectPermissionLevel level)
	{
		
		TridasGenericField genericField = new TridasGenericField();
		genericField.setName(INCOMPLETE_TRIDAS_ENTITY_PLACEHOLDER_GENERICFIELD_NAME);
		
		genericField.setValue(entityLabel 
				+ " - The entity has been removed. "
				+ "You are currently authorised to view no more than the "
				+ level.toString() + 
				" level of this project. ");
		return genericField;
	}
	
	protected void addPlaceholders(ITridasGeneric entity, String replaceEntityType,  int numToReplace, ProjectPermissionLevel level)
	{
		for(int i = 0; i < numToReplace; i++)
		{
			String label = String.format("(%s) %d of %d ", replaceEntityType, i+1, numToReplace);
			entity.getGenericFields().add(createRemovedTridasEntityPlaceholder(label, level));
		}
	}

	protected void restrictToPermitted(TridasObject object, ProjectPermissionLevel level)
	{
		List<TridasElement> elements = object.getElements();
		if(!ProjectPermissionLevel.ELEMENT.isPermittedBy(level))
		{
			addPlaceholders(object, MAP_ENTITY_TO_DISPLAYSTRING.get(TridasElement.class), elements.size(), level);
			elements.clear();
		}
		else
		{
			for(TridasElement element : elements)
				restrictToPermitted(element, level);
		}
		
		// sub-objects
		for(TridasObject subObject : object.getObjects())
			restrictToPermitted(subObject, level); // recursion!
	}
	
	protected void restrictToPermitted(TridasElement element, ProjectPermissionLevel level)
	{
		List<TridasSample> samples = element.getSamples();
		if(!ProjectPermissionLevel.SAMPLE.isPermittedBy(level))
		{
			addPlaceholders(element, MAP_ENTITY_TO_DISPLAYSTRING.get(TridasSample.class), samples.size(), level);
			samples.clear();
		}
		else
		{
			for (TridasSample sample : samples)
				 restrictToPermitted(sample, level);
		}		
	}
	
	protected void restrictToPermitted(TridasSample sample, ProjectPermissionLevel level)
	{
		List<TridasRadius> radiuses = sample.getRadiuses();
		if(!ProjectPermissionLevel.RADIUS.isPermittedBy(level))
		{
			addPlaceholders(sample, MAP_ENTITY_TO_DISPLAYSTRING.get(TridasRadius.class), radiuses.size(), level);
			radiuses.clear();
		}
		else
		{
			for(TridasRadius radius : radiuses)
				restrictToPermitted(radius, level);
		}		
	}
	
	protected void restrictToPermitted(TridasRadius radius, ProjectPermissionLevel level)
	{
		List<TridasMeasurementSeries> series = radius.getMeasurementSeries();
		if(!ProjectPermissionLevel.SERIES.isPermittedBy(level))
		{
			addPlaceholders(radius, MAP_ENTITY_TO_DISPLAYSTRING.get(TridasMeasurementSeries.class), series.size(), level);
			series.clear();
		}
		else
		{
			for(TridasMeasurementSeries serie : series)
				restrictToPermitted(serie, level);
		}
	}
	
	protected void restrictToPermitted(TridasMeasurementSeries serie, ProjectPermissionLevel level)
	{
		List<TridasValues> values = serie.getValues();
		if(!ProjectPermissionLevel.VALUES.isPermittedBy(level))
		{
			String replaceEntityType = MAP_ENTITY_TO_DISPLAYSTRING.get(values.getClass());
			addPlaceholders(serie, replaceEntityType, values.size(), level);
			values.clear();
		}
	}
	
	protected void restrictToPermitted(TridasDerivedSeries serie, ProjectPermissionLevel level)
	{
		List<TridasValues> values = serie.getValues();
		if(!ProjectPermissionLevel.VALUES.isPermittedBy(level))
		{
			String replaceEntityType = MAP_ENTITY_TO_DISPLAYSTRING.get(values.getClass());
			addPlaceholders(serie, replaceEntityType, values.size(), level);
			values.clear();
		}
	}
}
