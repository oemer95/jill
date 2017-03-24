package io.github.agentsoz.jill.lang;

/*
 * #%L
 * Jill Cognitive Agents Platform
 * %%
 * Copyright (C) 2014 - 2017 by its authors. See AUTHORS file.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import io.github.agentsoz.jill.Main;
import io.github.agentsoz.jill.core.GlobalState;
import io.github.agentsoz.jill.core.beliefbase.Belief;
import io.github.agentsoz.jill.core.beliefbase.BeliefBase;
import io.github.agentsoz.jill.core.beliefbase.BeliefBaseException;
import io.github.agentsoz.jill.core.beliefbase.BeliefSetField;
import io.github.agentsoz.jill.struct.AObject;
import io.github.agentsoz.jill.util.AObjectCatalog;
import io.github.agentsoz.jill.util.Log;
import io.github.agentsoz.jill.util.Stack255;

import java.io.PrintStream;
import java.util.HashSet;

/**
 * Base class for all BDI-like agents in the system. 
 * @author dsingh
 *
 */
public class Agent extends AObject {
	
	// FIXME: some Agent method are just by Jill only and should be final

	/**
	 * References to this agent's top level goals. 
	 * <p>
	 * For space efficiency, instead of storing
	 * references to {@link Goal} objects, we store catalog IDs for goals in 
	 * the {@link io.github.agentsoz.jill.core.GlobalState#goalTypes} catalog.
	 * Since these references are of size byte, a maximum of 255 goal
	 * types are supported in the system (globally, not per agent type). 
	 * These 1-byte references give very significant savings in space over 
	 * Java object references, which on a 64-bit machine take 8-bytes each.
	 * <p>
	 * NOTE: If a limit of 255 goal types in the system turns out to be 
	 * insufficient, then an agent type reference could be added here (at the 
	 * cost of another byte) and that would then allow 255 goal types 
	 * per agent type.
	 * 
	 */
	private byte[] goals; // This agent's goal-plan tree

	/**
	 * This agent's intention stack.
	 * <p>
	 * TODO: Currently, only a single intention stack is supported, which means
	 * that an agent can only really have one top level goal instance. This is
	 * to be extended to allow one stack per top level goal instance.
	 * <p>
	 * NOTE: This stack is of type {@link io.github.agentsoz.jill.util.Stack255} 
	 * which supports a maximum of 255 objects. This means that the active 
	 * goal-plan execution trace cannot be longer that 255. This should be more
	 * than enough for even the most complex goal-plan trees, but can be an 
	 * issue for recursive behaviours where a plan posts an instance of the 
	 * same goal type that it handles.
	 */
	private Stack255 executionStack; 

	/**
	 * A handle to any registered meta-planning function
	 */
	private MetaPlan metaplan;
	
	private static BeliefBase beliefbase;
	private static AObjectCatalog agents;
	private HashSet<Belief> lastresult;
	
	/** 
	 * Creates a new agent with the given name.
	 * @param name the name of this agent; consider using concise names when
	 * dealing with very large numbers of agents
	 */
	public Agent(String name) {
		super(name);
		executionStack = new Stack255((byte)8,(byte)2);
		beliefbase = GlobalState.beliefbase;
		agents = GlobalState.agents;
		lastresult = null;
		metaplan = null;
	}

	/**
	 * Gets the {@link #executionStack} of this agent. 
	 * @return this agent's execution stack
	 */
	public Stack255 getExecutionStack() {
		return executionStack;
	}

	/**
	 * Posts the given goal. This will trigger the BDI execution engine to 
	 * generate applicable plan instances to handle this goal. One instance,
	 * from the available options, will be selected and executed.
	 * @param goal the goal that this agent should try to achieve
	 */
	public void post(Goal goal) {
		synchronized(executionStack) {
			Log.debug("Agent "+getId()+" posting goal " + goal.getClass().getSimpleName());
			executionStack.push(goal);
			Main.setAgentIdle(getId(), false);
		}
		Main.flagMessageTo(Main.poolid(getId()));
	}
	
	public boolean send(int id, Goal msg) {
		AObject obj = agents.get(id);
		if (obj == null) {
			Log.warn("Agent " + getId() + " attempted to send a message to unknown agent id '"+id+"'");
			return false;
		}
		Log.debug("Agent " + getId() + " is sending message of type "+msg.getClass().getSimpleName()+" to agent "+id);
		((Agent)obj).post(msg);
		return true;
	}
	
	public boolean send(String name, Goal msg) {
		AObject obj = agents.find(name);
		if (obj == null) {
			Log.warn("Agent " + getId() + " attempted to send a message to unknown agent '"+name+"'");
			return false;
		}
		((Agent)obj).post(msg);
		return true;
	}
	
	public void start(PrintStream writer, String[] params) {
		Log.debug("Agent "+getId()+" is starting");
	}
	
	public void finish() {
		Log.debug("Agent "+getId()+" is finishing");
	}

	/** 
	 * Returns this agent's top level goals.
	 * @return {@link Agent#goals}
	 */
	public byte[] getGoals() {
		byte[] arr = new byte[goals.length];
		for (int i = 0; i < arr.length; i++) {
			arr[i] = goals[i];
		}
		return arr;
	}

	/**
	 * Set's this agent's top level goals, i.e., {@link #goals}.
	 * @param bs an array of goal IDs (must exist in the 
	 * {@link io.github.agentsoz.jill.core.GlobalState#goalTypes} catalog.
	 */
	public void setGoals(byte[] bs) {
		goals = new byte[bs.length];
		for (int i = 0; i < bs.length; i++) {
			goals[i] = (byte)(bs[i] & 0x000f);
		}
	}

	/**
	 * Creates a new belief set with the given fields.
	 * <p>
	 * Example usage:
	 * <pre>
	 * BeliefSetField[] fields = {
	 * 	new BeliefSetField("name", String.class, true),
	 * 	new BeliefSetField("gender", String.class, false),
	 * };
	 * createBeliefSet("neighbour", fields);
	 * </pre>
	 * @param name a name for this new belief set 
	 * @param fields an array of belief set fields (see {@link BeliefSetField}) 
	 * @throws BeliefBaseException see {@link BeliefSetField}
	 */
	public void createBeliefSet(String name, BeliefSetField[] fields) throws BeliefBaseException {
		beliefbase.createBeliefSet(getId(), name, fields);
	}
	
	/** 
	 * Adds a new belief to the specified belief set
	 * @param beliefsetName the belief set to add the belief to; must have been created previously using {@link #createBeliefSet(String, BeliefSetField[])}
	 * @param tuple parameter list of field values; types must match the specification in {@link #createBeliefSet(String, BeliefSetField[])}
	 * @throws BeliefBaseException exception (see {@link BeliefBaseException})
	 */
	public void addBelief(String beliefsetName, Object... tuple) throws BeliefBaseException {
		beliefbase.addBelief(getId(), beliefsetName, tuple);
	}
	
	public boolean eval(String query) throws BeliefBaseException {
		boolean result = beliefbase.eval(getId(), query);
		lastresult = (result) ? beliefbase.query(getId(), query) : new HashSet<Belief>();
		return result;
	}
	
	public HashSet<Belief> getLastResults() {
		return lastresult;
	}

	public void clearLastResults() {
		if (lastresult != null) {
			lastresult.clear();
		}
		lastresult = null;
	}
	
	/** 
	 * Forces this agent to enter an idle state irrespective of whether
	 * it has any active intentions or not. The agent will continue to 
	 * remain in the suspected state until some event forces it to become
	 * active again, at which point it will resume operation.
	 * @param val the new idle state of this agent
	 */
	public void suspend(boolean val) {
		Main.setAgentIdle(getId(), val);
		Main.flagMessageTo(Main.poolid(getId()));
	}
	
	/**
	 * Registers a meta-planning function for this agent.
	 * The registered function will be called by the Jill engine
	 * with the available plan bindings, prior to any plan selection,
	 * giving the agent the opportunity to do meta-level reasoning.
	 * @param metaplan the metaplan instance to register
	 */
	public void registerMetaPlan(MetaPlan metaplan) {
		this.metaplan = metaplan;
	}
	
	/**
	 * Called by the Jill engine with available plan bindings, 
	 * to Allow this agent to perform meta-level reasoning.
	 * @param bindings the available plan bindings
	 */
	public final void notifyAgentPrePlanSelection(PlanBindings bindings) {
		if (metaplan == null) return;
		metaplan.consider(bindings);
	}

}
