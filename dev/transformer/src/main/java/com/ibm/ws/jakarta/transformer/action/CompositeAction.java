package com.ibm.ws.jakarta.transformer.action;

import java.util.List;

public interface CompositeAction extends Action {
	List<? extends Action> getActions();

	Action acceptAction(String resourceName);
	Action getAcceptedAction();
}