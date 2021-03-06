package graphql.servlet

import com.google.common.io.ByteStreams
import graphql.Scalars
import graphql.schema.*

class TestUtils {

    static def createServlet(DataFetcher queryDataFetcher = { env -> env.arguments.arg },
                             DataFetcher mutationDataFetcher = { env -> env.arguments.arg }) {
        return SimpleGraphQLHttpServlet.newBuilder(createGraphQlSchema(queryDataFetcher, mutationDataFetcher)).build()
    }

    static def createGraphQlSchema(DataFetcher queryDataFetcher = { env -> env.arguments.arg },
                                   DataFetcher mutationDataFetcher = { env -> env.arguments.arg }) {
        GraphQLObjectType query = GraphQLObjectType.newObject()
                .name("Query")
                .field { GraphQLFieldDefinition.Builder field ->
            field.name("echo")
            field.type(Scalars.GraphQLString)
            field.argument { argument ->
                argument.name("arg")
                argument.type(Scalars.GraphQLString)
            }
            field.dataFetcher(queryDataFetcher)
        }
        .field { GraphQLFieldDefinition.Builder field ->
            field.name("returnsNullIncorrectly")
            field.type(new GraphQLNonNull(Scalars.GraphQLString))
            field.dataFetcher({ env -> null })
        }
        .build()

        GraphQLObjectType mutation = GraphQLObjectType.newObject()
                .name("Mutation")
                .field { field ->
            field.name("echo")
            field.type(Scalars.GraphQLString)
            field.argument { argument ->
                argument.name("arg")
                argument.type(Scalars.GraphQLString)
            }
            field.dataFetcher(mutationDataFetcher)
        }
                .field { field ->
            field.name("echoFile")
            field.type(Scalars.GraphQLString)
            field.argument { argument ->
                argument.name("file")
                argument.type(ApolloScalars.Upload)
            }
            field.dataFetcher( { env -> new String(ByteStreams.toByteArray(env.arguments.file.getInputStream())) } )
        }
                .field { field ->
            field.name("echoFiles")
            field.type(GraphQLList.list(Scalars.GraphQLString))
            field.argument { argument ->
                argument.name("files")
                argument.type(GraphQLList.list(GraphQLNonNull.nonNull(ApolloScalars.Upload)))
            }
            field.dataFetcher( { env -> env.arguments.files.collect { new String(ByteStreams.toByteArray(it.getInputStream())) } } )
        }
        .build()

        return GraphQLSchema.newSchema()
                            .query(query)
                            .mutation(mutation)
                            .additionalType(ApolloScalars.Upload)
                            .build()
    }

}
