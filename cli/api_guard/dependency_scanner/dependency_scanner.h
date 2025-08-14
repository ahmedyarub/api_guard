#ifndef DEPENDENCY_SCANNER_H
#define DEPENDENCY_SCANNER_H
#include <boost/graph/adjacency_list.hpp>
#include <map>
#include <string>
#include <utility>

#include "../models/DataHub.h"

using namespace boost;

typedef adjacency_list<vecS, vecS, directedS, DataHub, Link> Graph;
typedef graph_traits<Graph>::vertex_descriptor Vertex;

class DependencyScanner
{
public:
    explicit DependencyScanner(std::string java_analyzer_path, std::string javafx_path)
        : javaAnalyzerPath(std::move(java_analyzer_path)), javaFxPath(std::move(javafx_path))
    {
    }

    Graph scanDependencies(const std::string& projects_root);

private:
    Graph dhGraph;
    std::vector<DataHub> dataHubs;
    std::map<std::string, DataHub*> fqnDataHub;
    std::map<std::string, Vertex> adj;
    std::string javaAnalyzerPath;
    std::string javaFxPath;

    void addVertex(const DataHub* dh);
    void addEdge(const Consumer& cons, const Producer& prod);
};

#endif //DEPENDENCY_SCANNER_H
