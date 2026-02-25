#ifndef DEPENDENCY_SCANNER_H
#define DEPENDENCY_SCANNER_H
#include <boost/graph/adjacency_list.hpp>
#include <map>
#include <string>

#include "../models/DataHub.h"

using namespace boost;

typedef adjacency_list<vecS, vecS, directedS, DataHub, Link> Graph;
typedef graph_traits<Graph>::vertex_descriptor Vertex;

class DependencyScanner
{
public:
    Graph scanDependencies(const std::string& projects_root);

private:
    Graph dhGraph;
    std::vector<DataHub> dataHubs;
    std::map<std::string, DataHub*> fqnDataHub;
    std::map<std::string, Vertex> adj;

    void addVertex(const DataHub* dh);
    void addEdge(const Consumer& cons, const Producer& prod);
};

#endif //DEPENDENCY_SCANNER_H
