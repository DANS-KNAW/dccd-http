package nl.knaw.dans.dccd.rest.tridas;

import java.util.List;

import nl.knaw.dans.dccd.model.ProjectPermissionLevel;

import org.tridas.schema.TridasDerivedSeries;
import org.tridas.schema.TridasGenericField;
import org.tridas.schema.TridasObject;
import org.tridas.schema.TridasProject;

public class TridasRequestedLevelRestrictor extends TridasPermissionRestrictor 
{
	protected TridasGenericField createIncompleteTridasNote(ProjectPermissionLevel level)
	{
		TridasGenericField genericField = new TridasGenericField();
		genericField.setName(INCOMPLETE_TRIDAS_NOTE_GENERICFIELD_NAME);
		genericField.setValue("This TRiDaS is incomplete. "
				+ "You requested to view no more than the "
				+ level.toString() + 
				" level of this project. ");
		return genericField;
	}
	
	protected TridasGenericField createRemovedTridasEntityPlaceholder(String entityLabel, ProjectPermissionLevel level)
	{
		TridasGenericField genericField = new TridasGenericField();
		genericField.setName(INCOMPLETE_TRIDAS_ENTITY_PLACEHOLDER_GENERICFIELD_NAME);
		
		genericField.setValue(entityLabel 
				+ " - The entity has been removed. "
				+ "You requested to view no more than the "
				+ level.toString() + 
				" level of this project. ");
		return genericField;
	}
	
	// the 'minimal' level gives more information than 'project' level.
	// It is not an entity level, but we allow to use it.
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
				removeObjects(tridasProject, level);
				removeDerivedSeries(tridasProject, level);
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
	
	private void removeObjects(TridasProject tridasProject, ProjectPermissionLevel level)
	{
		List<TridasObject> objects = tridasProject.getObjects();
		addPlaceholders(tridasProject, MAP_ENTITY_TO_DISPLAYSTRING.get(TridasObject.class), objects.size(), level);
		// Note that sub-objects don't get a placeholder!
		objects.clear();
	}
}
