#ifndef DATAHUB_H
#define DATAHUB_H
#include <vector>
#include "Link.h"

class DataHub
{
public:
    std::string groupId;
    std::string artifactId;
    std::string rootFolder;
    std::vector<Link> links;

    [[nodiscard]] std::string getFqn() const
    {
        return groupId + "." + artifactId;
    }
};

#endif //DATAHUB_H
