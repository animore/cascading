/*
 * Copyright (c) 2007-2014 Concurrent, Inc. All Rights Reserved.
 *
 * Project and contact information: http://www.cascading.org/
 *
 * This file is part of the Cascading project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cascading.flow.tez.planner.rule.expression;

import cascading.flow.planner.iso.expression.ElementCapture;
import cascading.flow.planner.iso.expression.ExpressionGraph;
import cascading.flow.planner.iso.expression.FlowElementExpression;
import cascading.flow.planner.iso.expression.ScopeExpression;
import cascading.flow.planner.iso.expression.TypeExpression;
import cascading.flow.planner.rule.RuleExpression;
import cascading.flow.planner.rule.expressiongraph.SplicePipeExpressionGraph;
import cascading.flow.tez.planner.rule.expressiongraph.NoSpliceTapExpressionGraph;
import cascading.pipe.Group;
import cascading.pipe.Merge;
import cascading.pipe.Pipe;
import cascading.pipe.Splice;

/**
 *
 */
public class BalanceGroupSplitSpliceExpression extends RuleExpression
  {
  public static final FlowElementExpression GROUP = new FlowElementExpression( Group.class );
  public static final FlowElementExpression SPLICE = new FlowElementExpression( Splice.class );

  public BalanceGroupSplitSpliceExpression()
    {
    super(
      new NoSpliceTapExpressionGraph(),

      new ExpressionGraph()
        .arcs(
          GROUP,
          SPLICE )
        .arcs(
          GROUP,
          SPLICE ),

      new ExpressionGraph()
        .arcs(
          new FlowElementExpression( ElementCapture.Primary, Pipe.class, TypeExpression.Topo.SplitOnly )
        )
    );
    }
  }
