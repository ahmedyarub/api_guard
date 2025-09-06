#ifndef PROCESS_HANDLER_H
#define PROCESS_HANDLER_H
#include <cxxopts.hpp>
#include <string>
#include <boost/process/v2/popen.hpp>

std::string runProcess(const boost::process::v2::filesystem::path& command, const std::vector<std::string>& args, const std::vector<std::string>& envVariables);
std::filesystem::path getExecutablePath(const std::string& exe);

#endif //PROCESS_HANDLER_H
