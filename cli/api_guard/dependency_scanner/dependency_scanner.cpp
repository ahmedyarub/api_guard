#include "dependency_scanner.h"

#include <iostream>
#include <nlohmann/json.hpp>
#include "../process_handler/process_handler.h"
#include "../models/Consumer.h"
#include "../models/DataHub.h"
#include "../models/Link.h"
#include "../models/Producer.h"

class DataHub;
class Link;
class Producer;
class Consumer;
using json = nlohmann::json;

void from_json(const json& j, Consumer& c)
{
    j.at("clientClass").get_to(c.clientClass);
    j.at("parentFqn").get_to(c.parentFqn);
}

void from_json(const json& j, Producer& p)
{
    j.at("controller").get_to(p.controller);
    j.at("parentFqn").get_to(p.parentFqn);
}

void from_json(const json& j, Link& l)
{
    j.at("consumer").get_to(l.consumer);
    j.at("producer").get_to(l.producer);
}

void from_json(const json& j, DataHub& d)
{
    j.at("artifactId").get_to(d.artifactId);
    j.at("groupId").get_to(d.groupId);
    j.at("rootFolder").get_to(d.rootFolder);
    j.at("links").get_to(d.links);
}


void DependencyScanner::addVertex(const DataHub* dh)
{
    if (!adj.contains(dh->getFqn()))
    {
        const auto v = add_vertex(dhGraph);
        dhGraph[v] = *dh;
        adj[dh->getFqn()] = v;
    }
}

void DependencyScanner::addEdge(const Consumer& cons, const Producer& prod)
{
    // the consumer depends on the producer that's why the producer should be processed first
    const auto fromDh = fqnDataHub[prod.parentFqn];
    const auto toDh = fqnDataHub[cons.parentFqn];

    addVertex(fromDh);
    addVertex(toDh);

    add_edge(adj[fromDh->getFqn()], adj[toDh->getFqn()], dhGraph);
}

Graph DependencyScanner::scanDependencies(const std::string& projects_root)
{
    auto javaExePath = getExecutablePath("java");

    auto result = runProcess(
        javaExePath, {
            "--module-path", javaFxPath + "/lib",
            "--add-modules", "javafx.controls",
            "-jar",
            javaAnalyzerPath,
            "--cli",
            "--path=" + projects_root
        }, {}
    );

    try
    {
        const auto j = json::parse(result);

        dataHubs = j.get<std::vector<DataHub>>();
    }
    catch (const std::exception& e)
    {
        std::cerr << e.what() << '\n';
    }

    for (auto& dataHub : dataHubs)
    {
        fqnDataHub[dataHub.getFqn()] = &dataHub;
    }

    for (auto& dataHub : dataHubs)
    {
        for (auto& [consumer, producer] : dataHub.links)
        {
            addEdge(consumer, producer);
        }
    }

    return dhGraph;
}
