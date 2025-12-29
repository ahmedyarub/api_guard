#ifndef CONSUMER_H
#define CONSUMER_H
#include <string>

class DataHub;

class Consumer
{
public:
    std::string clientClass;
    std::string parentFqn;
};

#endif //CONSUMER_H
