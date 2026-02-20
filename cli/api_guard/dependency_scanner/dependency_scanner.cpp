// Copyright 2026 Ahmed Yarub Hani Al Nuaimi
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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
        const auto v = boost::add_vertex(dhGraph);
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

    boost::add_edge(adj[fromDh->getFqn()], adj[toDh->getFqn()], dhGraph);
}

Graph DependencyScanner::scanDependencies(const std::string& projects_root)
{
    const auto javaExePath = getExecutablePath("java");
    if (javaExePath.empty())
    {
        std::cerr << "Error: 'java' executable not found on PATH." << std::endl;
        return dhGraph;
    }

    auto result = runProcess(
        javaExePath, {
            "--enable-native-access=javafx.graphics",
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
