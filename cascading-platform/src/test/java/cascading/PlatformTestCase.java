/*
 * Copyright (c) 2007-2013 Concurrent, Inc. All Rights Reserved.
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

package cascading;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cascading.flow.FlowConnectorProps;
import cascading.operation.DebugLevel;
import cascading.platform.PlatformRunner;
import cascading.platform.TestPlatform;
import cascading.util.Util;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PlatformTestCase is the base class for JUnit tests that are platform agnostic. That is using the {@link TestPlatform}
 * interface each test can be run against all supported platform like Hadoop or Cascading local mode.
 * <p/>
 * It is strongly recommended users look at the source of {@link FieldedPipesPlatformTest} or related tests to see how
 * this class is used.
 * <p/>
 * This test case uses the {@link PlatformRunner} to inject the available platform providers which implement the
 * TestPlatform base class.
 * <p/>
 * By default the PlatformRunner looks for "cascading/platform/platform.properties" file in the classpath, and
 * instantiates the class specified by the "platform.classname" property. If more than one "platform.properties"
 * resource is found, each class is instantiated and the whole suite of tests will be run against each instance.
 * <p/>
 * To limit this, setting the system property "platform.includes" to list the platform names that should be run will
 * cause the PlatformRunner to ignore any unlisted platforms. Thus setting {@code platform.includes=local}, only
 * local mode will run even if the "hadoop" platform was found in the classpath.
 * <p/>
 * To pass custom properties to each test to be used by the {@link cascading.flow.FlowConnector}, create
 * system properties prefixed by "platform.". These properties, minus the "platform." prefix in the property name,
 * will override any defaults.
 * <p/>
 * Subclasses of PlatformTestCase can set "{@code useCluster} to {@code true} on the constructor if the underlying
 * platform can boot a cluster for testing. By setting the system property "test.cluster.enabled" to false, this
 * can be deactivated in order to temporarily speed test execution. By default {@code useCluster} is {@code false},
 * typically user tests don't need to have a cluster running to test their functionality so leaving the default is
 * reasonable.
 */
@RunWith(PlatformRunner.class)
public class PlatformTestCase extends CascadingTestCase
  {
  private static final Logger LOG = LoggerFactory.getLogger( PlatformTestCase.class );

  public static final String ROOT_OUTPUT_PATH = "test.output.root";
  static Set<String> allPaths = new HashSet<String>();

  private String rootPath;
  Set<String> currentPaths = new HashSet<String>();

  @Rule
  public transient TestName name = new TestName();

  private transient TestPlatform platform = null;

  private transient boolean useCluster;
  private transient int numMapTasks;
  private transient int numReduceTasks;

  public PlatformTestCase( boolean useCluster )
    {
    this.useCluster = useCluster;
    }

  public PlatformTestCase( boolean useCluster, int numMapTasks, int numReduceTasks )
    {
    this( useCluster );
    this.numMapTasks = numMapTasks;
    this.numReduceTasks = numReduceTasks;
    }

  public PlatformTestCase()
    {
    this( false );
    }

  public void installPlatform( TestPlatform platform )
    {
    this.platform = platform;
    this.platform.setUseCluster( useCluster );

    if( this.platform.isMapReduce() )
      {
      platform.setNumMappers( numMapTasks );
      platform.setNumReducers( numReduceTasks );
      }
    }

  public TestPlatform getPlatform()
    {
    return platform;
    }

  public String getTestName()
    {
    return name.getMethodName();
    }

  protected String getRootPath()
    {
    if( rootPath == null )
      rootPath = Util.join( getPathElements(), "/" );

    return rootPath;
    }

  protected String[] getPathElements()
    {
    return new String[]{getTestRoot(), getPlatformName(), getTestCaseName()};
    }

  public String getOutputPath( String path )
    {
    String result = makeOutputPath( path );

    if( allPaths.contains( result ) )
      throw new IllegalStateException( "path already has been used:" + result );

    allPaths.add( result );
    currentPaths.add( result );

    return result;
    }

  private String makeOutputPath( String path )
    {
    if( path.startsWith( "/" ) )
      return getRootPath() + path;

    return getRootPath() + "/" + path;
    }

  public String getTestCaseName()
    {
    return getClass().getSimpleName().replaceAll( "^(.*)Test.*$", "$1" ).toLowerCase();
    }

  public String getPlatformName()
    {
    return platform.getName();
    }

  public static String getTestRoot()
    {
    return System.getProperty( ROOT_OUTPUT_PATH, "build/test/output" );
    }

  @Before
  public void setUp() throws Exception
    {
    getPlatform().setUp();
    }

  public Map<Object, Object> getProperties()
    {
    return new HashMap<Object, Object>( getPlatform().getProperties() );
    }

  protected void copyFromLocal( String inputFile ) throws IOException
    {
    getPlatform().copyFromLocal( inputFile );
    }

  protected Map<Object, Object> disableDebug()
    {
    Map<Object, Object> properties = getProperties();
    FlowConnectorProps.setDebugLevel( properties, DebugLevel.NONE );

    return properties;
    }

  @After
  public void tearDown() throws Exception
    {
    try
      {
      for( String path : currentPaths )
        {
        LOG.info( "copying to local {}", path );

        if( getPlatform().isUseCluster() && getPlatform().remoteExists( path ) )
          getPlatform().copyToLocal( path );
        }

      currentPaths.clear();
      }
    finally
      {
      getPlatform().tearDown();
      }
    }
  }
