package agentsoz.jill.example.tokenpassing;

/*
 * #%L
 * Jill Cognitive Agents Platform
 * %%
 * Copyright (C) 2014 - 2015 by its authors. See AUTHORS file.
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

import java.io.PrintStream;

import agentsoz.jill.core.GlobalState;
import agentsoz.jill.core.beliefbase.BeliefBaseException;
import agentsoz.jill.core.beliefbase.BeliefSetField;
import agentsoz.jill.example.greeter.BeFriendly;
import agentsoz.jill.lang.Agent;
import agentsoz.jill.lang.AgentInfo;
import agentsoz.jill.util.Log;

@AgentInfo(hasGoals={"agentsoz.jill.example.tokenpassing.Token3"})
public class TokenAgent3 extends Agent {

	public static PrintStream out;
	private static final String beliefset = "neighbour";

	// Defaults 
	public static int rounds = 1;
	private static int neighbourhood = 1;

	public TokenAgent3(String str) {
		super(str);
	}

	@Override
	public void start(PrintStream writer, String[] params) {
		parse(params);
		out = writer;
		
		// Create a new belief set about neighbours
		BeliefSetField[] fields = {
				new BeliefSetField("name", String.class, true),
		};

		try {
			// Attach this belief set to this agent
			this.createBeliefSet(beliefset, fields);
		
			// Add beliefs about neighbours
			int numAgents = GlobalState.agents.size();
            for (int i=-neighbourhood; i<=neighbourhood; i++){
            	if (i==0) { continue; }
            	this.addBelief(beliefset, Integer.toString((getId()+i+numAgents)%numAgents));
            }
            Log.debug("Agent " + getName() + " is initialising with neighbourhood size of " + neighbourhood + " on each side (so "+(neighbourhood*2)+" neighbours)");

    		// Let Agent 0 start the token passing
    		if (getId() == 0) {
    			Log.info("round 1");
    			Token3 token = new Token3(1,1, System.currentTimeMillis());
    			token.setHops(1);
    			send(1, token);
    		}
		} catch (BeliefBaseException e) {
			Log.error(e.getMessage());
		}
	}

    public static void parse(String[] args) {
        for (int i = 0; i < args.length; i++) {
        	switch (args[i]) {
        	case "-neighbourhood":
        		if (i + 1 < args.length) {
        			i++;
        			try {
        				neighbourhood = Integer.parseInt(args[i]);
        			} catch (Exception e) {
        				Log.warn("Value '" + args[i] + "' is not a number");
        			}
        		}
        		break;
        	case "-rounds":
        		if (i + 1 < args.length) {
        			i++;
        			try {
        				rounds = Integer.parseInt(args[i]);
        			} catch (Exception e) {
        				Log.warn("Value '" + args[i] + "' is not a number");
        			}
        		}
        		break;
        	}
        }
    }
}
