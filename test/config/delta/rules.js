export default [
    {
      match: {
        // form of element is {subject,predicate,object}
        // predicate: { type: "uri", value: "http://www.semanticdesktop.org/ontologies/2007/03/22/nmo#isPartOf" }
      },
      callback: {
        url: "http://kalliope-api/delta", method: "POST"
      },
      options: {
        resourceFormat: "v0.0.1",
        gracePeriod: 1000,
        ignoreFromSelf: true
      }
    }
  ]