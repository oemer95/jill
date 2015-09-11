package io.github.agentsoz.jill.testprogram;

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

import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.AgentInfo;

import java.io.PrintStream;

@AgentInfo(hasGoals={"io.github.agentsoz.jill.testprogram.GoalA","io.github.agentsoz.jill.testprogram.GoalB","io.github.agentsoz.jill.testprogram.GoalC"})
public class TestAgent extends Agent {

	private int i = 0;
	private PrintStream writer = null;
	private boolean verbose = false;
	
	public TestAgent(String str) {
		super(str);
	}

	@Override
	public void start(PrintStream writer, String[] params) {
		if (params != null && params[0].equals("-d")) {
			verbose = true;
			this.writer = writer;
		}
		post(new GoalA("gA"));
	}
	
	@Override
	public void finish() {
		/*
		 * Check that some plan changed i.  Should print:
		 *   hex '8' if plan PlanD ran, 
		 *   hex '7' if plans PlanA then PlanB then PlanC ran. 
		*/
		if (verbose ) {
			if (writer == null) {
				System.out.printf("%h", i);
			} else {
				writer.printf("%h", i);
			}
		}
	}

	public int getI() {
		return i;
	}

	public void setI(int i) {
		this.i = i;
	}

}