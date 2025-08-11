#ifndef PRODUCER_H
#define PRODUCER_H
#include <string>

class DataHub;

class Producer
{
public:
    std::string controller;
    std::string parentFqn;
};

#endif //PRODUCER_H
