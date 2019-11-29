/*
 * Copyright DataStax, Inc.
 *
 * This software can be used solely with DataStax Enterprise. Please consult the license at
 * http://www.datastax.com/terms/datastax-dse-driver-license-terms
 */
package com.datastax.dse.driver.api.core.graph.remote;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.unfold;

import com.datastax.dse.driver.api.core.DseSession;
import com.datastax.dse.driver.api.core.graph.CoreGraphDataTypeITBase;
import com.datastax.dse.driver.api.core.graph.DseGraph;
import com.datastax.dse.driver.api.core.graph.GraphTestSupport;
import com.datastax.dse.driver.api.testinfra.session.DseSessionRule;
import com.datastax.oss.driver.api.testinfra.DseRequirement;
import com.datastax.oss.driver.api.testinfra.ccm.CustomCcmRule;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import java.util.Map;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.ClassRule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

@DseRequirement(min = "6.8.0", description = "DSE 6.8.0 required for Core graph support")
@RunWith(DataProviderRunner.class)
public class CoreGraphDataTypeRemoteIT extends CoreGraphDataTypeITBase {

  private static final CustomCcmRule CCM_RULE = GraphTestSupport.CCM_BUILDER_WITH_GRAPH.build();

  private static final DseSessionRule SESSION_RULE =
      GraphTestSupport.getCoreGraphSessionBuilder(CCM_RULE).build();

  @ClassRule
  public static final TestRule CHAIN = RuleChain.outerRule(CCM_RULE).around(SESSION_RULE);

  @Override
  protected DseSession session() {
    return SESSION_RULE.session();
  }

  @Override
  protected String graphName() {
    return SESSION_RULE.getGraphName();
  }

  private final GraphTraversalSource g =
      DseGraph.g.withRemote(DseGraph.remoteConnectionBuilder(session()).build());

  @Override
  public Map<Object, Object> insertVertexThenReadProperties(
      Map<String, Object> properties, int vertexID, String vertexLabel) {
    GraphTraversal<Vertex, Vertex> traversal = g.addV(vertexLabel).property("id", vertexID);

    for (Map.Entry<String, Object> entry : properties.entrySet()) {
      String typeDefinition = entry.getKey();
      String propName = formatPropertyName(typeDefinition);
      Object value = entry.getValue();
      traversal = traversal.property(propName, value);
    }

    // insert vertex
    traversal.iterate();

    // query properties
    return g.V().has(vertexLabel, "id", vertexID).valueMap().by(unfold()).next();
  }
}