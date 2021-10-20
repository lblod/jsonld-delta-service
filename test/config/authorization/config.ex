alias Acl.Accessibility.Always, as: AlwaysAccessible
alias Acl.GraphSpec.Constraint.Resource, as: ResourceConstraint
alias Acl.GraphSpec.Constraint.ResourceFormat, as: ResourceFormatConstraint
alias Acl.Accessibility.ByQuery, as: AccessByQuery
alias Acl.GraphSpec, as: GraphSpec
alias Acl.GroupSpec, as: GroupSpec
alias Acl.GroupSpec.GraphCleanup, as: GraphCleanup

defmodule Acl.UserGroups.Config do
  def user_groups do
    [
      %GroupSpec{
        name: "public",
        useage: [:read],
        access: %AlwaysAccessible{},
        graphs: [%GraphSpec{
          graph: "http://mu.semte.ch/graphs/public",
          constraint: %ResourceConstraint{
            resource_types: [
              # todo add classes
            ]
          }
        }]
      },
      %GroupSpec{
        name: "contacthub",
        useage: [:write, :read_for_write, :read],
        access: %AlwaysAccessible{},
        graphs: [%GraphSpec{
          graph: "http://mu.semte.ch/graphs/contacthub",
          constraint: %ResourceConstraint{
            resource_types: [
              "http://vocab.deri.ie/cogs#Job",
              "http://open-services.net/ns/core#Error",
              "http://lblod.data.gift/vocabularies/harvesting/HarvestingCollection",
              "http://www.semanticdesktop.org/ontologies/2007/03/22/nfo#RemoteDataObject",
              "http://www.semanticdesktop.org/ontologies/2007/03/22/nfo#FileDataObject",
              "http://www.semanticdesktop.org/ontologies/2007/03/22/nfo#DataContainer",
              "http://oscaf.sourceforge.net/ndo.html#DownloadEvent",
            ]
          }
        }]
      },
      %GraphCleanup{
        originating_graph: "http://mu.semte.ch/application",
        useage: [:read, :write],
        name: "clean"
      }
    ]
  end
end
